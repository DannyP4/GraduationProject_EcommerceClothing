package com.uniform.store.service.impl;

import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderResponse;
import com.uniform.store.entity.*;
import com.uniform.store.enums.DiscountType;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.*;
import com.uniform.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("75.00");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("8.99");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Address address = addressRepository.findByIdAndUserId(req.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", req.getAddressId()));

        // Calculate totals
        BigDecimal subtotal = calculateSubtotal(items);
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_FEE;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = calculateDiscount(cart.getCoupon(), subtotal);
        BigDecimal total = subtotal.add(shippingFee).add(taxAmount).subtract(discountAmount);

        // Build order
        Order order = Order.builder()
                .user(address.getUser())
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING_PAYMENT)
                .shippingName(address.getRecipientName())
                .shippingLine1(address.getAddressLine1())
                .shippingLine2(address.getAddressLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPostal(address.getPostalCode())
                .shippingCountry(address.getCountryCode())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(total)
                .coupon(cart.getCoupon())
                .couponCodeSnapshot(cart.getCoupon() != null ? cart.getCoupon().getCode() : null)
                .notes(req.getNotes())
                .build();

        order = orderRepository.save(order);

        // Create order items with snapshots + deduct stock
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();

            // Deduct stock (optimistic locking via @Version)
            if (variant.getStockQty() < cartItem.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for: " + product.getName() + " (" + variant.getSize() + ")");
            }
            variant.setStockQty(variant.getStockQty() - cartItem.getQuantity());
            variantRepository.save(variant);

            BigDecimal unitPrice = product.getBasePrice().add(variant.getPriceDelta());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            String imageUrl = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .productIdSnapshot(product.getId())
                    .productNameSnapshot(product.getName())
                    .productSlugSnapshot(product.getSlug())
                    .skuSnapshot(variant.getSku())
                    .sizeSnapshot(variant.getSize().name())
                    .colorSnapshot(variant.getColor())
                    .imageUrlSnapshot(imageUrl)
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);
        }
        order.setItems(orderItems);
        orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cart.setCoupon(null);
        cartRepository.save(cart);

        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, int page) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, 10))
                .map(this::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toOrderResponse(order);
    }

    // ── Helpers ──────────────────────────────────────────────

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

    private String generateOrderNumber() {
        return "UNI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderItemInfo> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemInfo.builder()
                        .id(i.getId())
                        .productName(i.getProductNameSnapshot())
                        .productSlug(i.getProductSlugSnapshot())
                        .size(i.getSizeSnapshot())
                        .color(i.getColorSnapshot())
                        .imageUrl(i.getImageUrlSnapshot())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .shippingName(order.getShippingName())
                .shippingLine1(order.getShippingLine1())
                .shippingLine2(order.getShippingLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPostal(order.getShippingPostal())
                .shippingCountry(order.getShippingCountry())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .couponCodeSnapshot(order.getCouponCodeSnapshot())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }
}
