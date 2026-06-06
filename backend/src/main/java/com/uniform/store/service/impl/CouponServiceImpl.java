package com.uniform.store.service.impl;

import com.uniform.store.dto.response.CouponValidationResponse;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Coupon;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CouponService;
import com.uniform.store.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final PricingService pricingService;

    @Override
    public CouponValidationResponse validate(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Coupon coupon = loadActiveLookup(code);

        Instant now = Instant.now();
        CartContext ctx = loadCartLines(user.getId(), now);
        BigDecimal discount = evaluate(coupon, ctx.lines, ctx.subtotal, user.getId(), now);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .type(coupon.getType())
                .scope(coupon.getScope())
                .value(coupon.getValue())
                .discountAmount(discount)
                .subtotal(ctx.subtotal)
                .totalAfterDiscount(ctx.subtotal.subtract(discount))
                .message("Coupon applied")
                .build();
    }

    @Override
    public CouponValidationResponse validateDirect(String email, String code, Long variantId, int quantity) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Coupon coupon = loadActiveLookup(code);

        Instant now = Instant.now();
        CartContext ctx = loadDirectLine(variantId, quantity, now);
        BigDecimal discount = evaluate(coupon, ctx.lines, ctx.subtotal, user.getId(), now);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .type(coupon.getType())
                .scope(coupon.getScope())
                .value(coupon.getValue())
                .discountAmount(discount)
                .subtotal(ctx.subtotal)
                .totalAfterDiscount(ctx.subtotal.subtract(discount))
                .message("Coupon applied")
                .build();
    }

    @Override
    @Transactional
    public CouponApplication applyToOrder(String code, Long userId, List<CartLine> lines, BigDecimal subtotal) {
        Coupon coupon = loadActiveLookup(code);
        BigDecimal discount = evaluate(coupon, lines, subtotal, userId, Instant.now());

        // Atomic guard against the last-use race: only commits if the cap still has room.
        if (couponRepository.incrementUsage(coupon.getId()) == 0) {
            throw new BadRequestException("This coupon has reached its usage limit.");
        }
        return new CouponApplication(coupon, discount);
    }

    @Override
    @Transactional
    public void releaseForOrder(Long orderId) {
        orderCouponRepository.findByOrderId(orderId).ifPresent(oc -> {
            couponRepository.decrementUsage(oc.getCoupon().getId());
            orderCouponRepository.deleteByOrderId(orderId);
        });
    }

    private Coupon loadActiveLookup(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return couponRepository.findByCode(normalized)
                .orElseThrow(() -> new BadRequestException("Invalid coupon code."));
    }

    private BigDecimal evaluate(Coupon c, List<CartLine> lines, BigDecimal subtotal, Long userId, Instant now) {
        if (c.getStatus() != CouponStatus.ACTIVE) {
            throw new BadRequestException("This coupon is not available.");
        }
        if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) {
            throw new BadRequestException("This coupon is not active yet.");
        }
        if (c.getEndsAt() != null && !now.isBefore(c.getEndsAt())) {
            throw new BadRequestException("This coupon has expired.");
        }
        if (c.getMaxUses() != null && c.getUsedCount() >= c.getMaxUses()) {
            throw new BadRequestException("This coupon has reached its usage limit.");
        }
        if (c.getMaxUsesPerUser() != null
                && orderCouponRepository.countByCouponAndUser(c.getId(), userId) >= c.getMaxUsesPerUser()) {
            throw new BadRequestException("You have already used this coupon.");
        }
        if (c.getMinOrderAmount() != null && subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Order does not meet the minimum for this coupon.");
        }

        BigDecimal eligible = eligibleSubtotal(c, lines, subtotal);
        if (eligible.signum() <= 0) {
            throw new BadRequestException("No items in your cart are eligible for this coupon.");
        }

        BigDecimal discount = c.getType() == CouponType.PERCENT
                ? eligible.multiply(c.getValue()).movePointLeft(2)
                : c.getValue();
        discount = discount.setScale(0, RoundingMode.HALF_UP);

        if (c.getMaxDiscountAmount() != null && discount.compareTo(c.getMaxDiscountAmount()) > 0) {
            discount = c.getMaxDiscountAmount().setScale(0, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(eligible) > 0) discount = eligible;
        if (discount.signum() < 0) discount = BigDecimal.ZERO;
        return discount;
    }

    private BigDecimal eligibleSubtotal(Coupon c, List<CartLine> lines, BigDecimal subtotal) {
        if (c.getScope() == CouponScope.ALL) return subtotal;
        Set<Long> ids = c.getScope() == CouponScope.CATEGORY ? c.getCategoryIds() : c.getProductIds();
        BigDecimal sum = BigDecimal.ZERO;
        for (CartLine l : lines) {
            Long key = c.getScope() == CouponScope.CATEGORY ? l.categoryId() : l.productId();
            if (key != null && ids.contains(key)) sum = sum.add(l.lineTotal());
        }
        return sum;
    }

    private CartContext loadCartLines(Long userId, Instant now) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty."));
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Your cart is empty.");
        }

        List<Long> variantIds = items.stream().map(i -> i.getVariant().getId()).toList();
        Map<Long, ProductVariant> vmap = variantRepository.findAllByIdInWithProduct(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartLine> lines = new ArrayList<>(items.size());
        for (CartItem ci : items) {
            ProductVariant v = vmap.get(ci.getVariant().getId());
            if (v == null) continue;
            Product p = v.getProduct();
            PricingService.EffectivePrice ep = pricingService.resolve(p, v, now);
            BigDecimal lineTotal = ep.effectivePrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            lines.add(new CartLine(p.getId(), p.getCategory().getId(), lineTotal));
            subtotal = subtotal.add(lineTotal);
        }
        return new CartContext(lines, subtotal);
    }

    private CartContext loadDirectLine(Long variantId, int quantity, Instant now) {
        ProductVariant v = variantRepository.findAllByIdInWithProduct(List.of(variantId)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        Product p = v.getProduct();
        PricingService.EffectivePrice ep = pricingService.resolve(p, v, now);
        BigDecimal lineTotal = ep.effectivePrice().multiply(BigDecimal.valueOf(quantity));
        return new CartContext(List.of(new CartLine(p.getId(), p.getCategory().getId(), lineTotal)), lineTotal);
    }

    private record CartContext(List<CartLine> lines, BigDecimal subtotal) {}
}
