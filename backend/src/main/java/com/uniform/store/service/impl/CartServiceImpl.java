package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.CartResponse;
import com.uniform.store.entity.*;
import com.uniform.store.enums.DiscountType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.*;
import com.uniform.store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("75.00");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("8.99");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest req) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findById(req.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant", req.getVariantId()));

        if (!variant.isActive()) {
            throw new BadRequestException("Variant is not available");
        }

        if (variant.getStockQty() < req.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + variant.getStockQty());
        }

        // Dedup: if same variant already in cart, increase quantity
        CartItem existing = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + req.getQuantity();
            if (variant.getStockQty() < newQty) {
                throw new BadRequestException("Insufficient stock. Available: " + variant.getStockQty());
            }
            existing.setQuantity((short) newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(req.getQuantity().shortValue())
                    .build();
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest req) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this cart");
        }

        if (item.getVariant().getStockQty() < req.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + item.getVariant().getStockQty());
        }

        item.setQuantity(req.getQuantity().shortValue());
        cartItemRepository.save(item);

        return buildCartResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this cart");
        }

        cartItemRepository.delete(item);
        return buildCartResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse applyPromo(Long userId, String code) {
        Cart cart = getOrCreateCart(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code)
                .orElseThrow(() -> new BadRequestException("Coupon not found or inactive: " + code));

        // Validate expiry
        Instant now = Instant.now();
        if (coupon.getValidFrom().isAfter(now)) {
            throw new BadRequestException("Coupon is not yet active");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(now)) {
            throw new BadRequestException("Coupon has expired");
        }

        // Student-only check
        if (coupon.isStudentOnly() && !user.isStudentVerified()) {
            throw new BadRequestException("This coupon is for verified students only");
        }

        // Minimum order amount
        BigDecimal subtotal = calculateSubtotal(cart.getItems());
        if (subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Minimum order amount for this coupon is $" + coupon.getMinimumOrderAmount());
        }

        cart.setCoupon(coupon);
        cartRepository.save(cart);

        return buildCartResponse(cart, userId);
    }

    @Override
    @Transactional
    public CartResponse removePromo(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setCoupon(null);
        cartRepository.save(cart);
        return buildCartResponse(cart, userId);
    }

    // ── Private helpers ──────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse buildCartResponse(Cart cart, Long userId) {
        // Reload cart with fresh items from DB
        cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", String.valueOf(userId)));

        BigDecimal subtotal = calculateSubtotal(cart.getItems());
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = calculateDiscount(cart.getCoupon(), subtotal);
        BigDecimal total = subtotal.add(shippingFee).add(taxAmount).subtract(discountAmount);

        List<CartResponse.CartItemInfo> items = cart.getItems().stream()
                .map(this::toCartItemInfo)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .total(total)
                .freeShipping(shippingFee.compareTo(BigDecimal.ZERO) == 0)
                .appliedCouponCode(cart.getCoupon() != null ? cart.getCoupon().getCode() : null)
                .appliedCouponDescription(cart.getCoupon() != null ? cart.getCoupon().getDescription() : null)
                .build();
    }

    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal price = item.getVariant().getProduct().getBasePrice()
                            .add(item.getVariant().getPriceDelta());
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null) return BigDecimal.ZERO;

        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscountCap() != null) {
                discount = discount.min(coupon.getMaximumDiscountCap());
            }
        } else {
            discount = coupon.getDiscountValue().min(subtotal);
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private CartResponse.CartItemInfo toCartItemInfo(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        BigDecimal unitPrice = product.getBasePrice().add(variant.getPriceDelta());

        String imageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl());

        return CartResponse.CartItemInfo.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .size(variant.getSize().name())
                .color(variant.getColor())
                .sku(variant.getSku())
                .imageUrl(imageUrl)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP))
                .stockQty(variant.getStockQty())
                .build();
    }
}
