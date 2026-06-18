package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import com.uniform.store.dto.request.DirectOrderRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.PlaceOrderResponse;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderCoupon;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.event.OrderEmailEvent;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.OrderMapper;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CouponService;
import com.uniform.store.service.FxService;
import com.uniform.store.service.OrderService;
import com.uniform.store.service.PricingService;
import com.uniform.store.service.ShippingService;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final String DEFAULT_CURRENCY = "VND";
    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final FxService fxService;
    private final VnpayService vnpayService;
    private final StripeService stripeService;
    private final OrderMapper orderMapper;
    private final PricingService pricingService;
    private final CouponService couponService;
    private final OrderCouponRepository orderCouponRepository;
    private final ShippingService shippingService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.orders.pending-timeout-minutes:30}")
    private long pendingTimeoutMinutes;

    @Override
    @Transactional
    public PlaceOrderResponse placeOrder(String email, PlaceOrderRequest req, String clientIp) {
        PaymentProvider provider = parseProvider(req.getPaymentMethod());
        User user = loadUser(email);
        Address address = loadAddress(user, req.getAddressId());

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<LineRequest> lines = items.stream()
                .map(ci -> new LineRequest(ci.getVariant().getId(), ci.getQuantity()))
                .toList();

        PlaceOrderResponse response = createOrder(user, address, lines,
                req.getCouponCode(), req.getNotes(), provider, clientIp);
        cartItemRepository.deleteAllByCartId(cart.getId());
        return response;
    }

    @Override
    @Transactional
    public PlaceOrderResponse placeDirectOrder(String email, DirectOrderRequest req, String clientIp) {
        PaymentProvider provider = parseProvider(req.getPaymentMethod());
        User user = loadUser(email);
        Address address = loadAddress(user, req.getAddressId());

        List<LineRequest> lines = List.of(new LineRequest(req.getVariantId(), req.getQuantity()));
        return createOrder(user, address, lines, req.getCouponCode(), req.getNotes(), provider, clientIp);
    }

    private PlaceOrderResponse createOrder(User user, Address address, List<LineRequest> lines,
                                           String couponCode, String notes,
                                           PaymentProvider provider, String clientIp) {
        if (provider == PaymentProvider.BANK_TRANSFER) {
            throw new BadRequestException("BANK_TRANSFER is not supported yet.");
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("No items to order");
        }

        List<Long> variantIds = lines.stream().map(LineRequest::variantId).toList();
        // pessimistic lock against concurrent placeOrder/cancelOrder on the same variant.
        Map<Long, ProductVariant> variants = variantRepository
                .findAllByIdInWithProductForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<String> blocked = new ArrayList<>();
        for (LineRequest line : lines) {
            ProductVariant v = variants.get(line.variantId());
            if (v == null || !isVariantAvailable(v)) {
                blocked.add(v != null ? v.getSku() : "variant#" + line.variantId());
                continue;
            }
            int stock = safeStock(v);
            if (stock < line.quantity()) {
                blocked.add(v.getSku() + " (need " + line.quantity() + ", left " + stock + ")");
            }
        }
        if (!blocked.isEmpty()) {
            throw new BadRequestException(
                    "Items unavailable: " + String.join(", ", blocked) + ". Please update your selection.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        List<OrderItem> orderItems = new ArrayList<>(lines.size());
        List<CouponService.CartLine> couponLines = new ArrayList<>(lines.size());
        Instant now = Instant.now();

        for (LineRequest line : lines) {
            ProductVariant v = variants.get(line.variantId());
            Product p = v.getProduct();
            PricingService.EffectivePrice ep = pricingService.resolve(p, v, now);
            BigDecimal unitPrice = ep.effectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));
            currency = p.getCurrency();

            orderItems.add(OrderItem.builder()
                    .variant(v)
                    .productName(p.getName())
                    .variantLabel(formatVariantLabel(v))
                    .sku(v.getSku())
                    .unitPrice(unitPrice)
                    .originalUnitPrice(ep.onSale() ? ep.originalPrice() : null)
                    .quantity(line.quantity())
                    .lineTotal(lineTotal)
                    .build());
            couponLines.add(new CouponService.CartLine(p.getId(), p.getCategory().getId(), lineTotal));

            v.setStockQuantity(v.getStockQuantity() - line.quantity());
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal shippingCost = shippingService.fee(address.getRegion(), subtotal);
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        CouponService.CouponApplication couponApp = null;
        if (couponCode != null && !couponCode.isBlank()) {
            couponApp = couponService.applyToOrder(couponCode, user.getId(), couponLines, subtotal);
            discountTotal = couponApp.discountAmount();
        }
        BigDecimal grandTotal = subtotal.add(shippingCost).add(taxTotal).subtract(discountTotal);

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.next())
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountTotal(discountTotal)
                .shippingCost(shippingCost)
                .taxTotal(taxTotal)
                .grandTotal(grandTotal)
                .currency(currency)
                .shippingRecipient(address.getRecipient())
                .shippingPhone(address.getPhone())
                .shippingLine1(address.getLine1())
                .shippingWard(address.getWard())
                .shippingDistrict(address.getDistrict())
                .shippingCity(address.getCity())
                .shippingCountry(address.getCountry())
                .shippingPostalCode(address.getPostalCode())
                .shippingRegion(address.getRegion())
                .notes(notes)
                .placedAt(Instant.now())
                .build();
        orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            orderItemRepository.save(oi);
        }

        if (couponApp != null) {
            orderCouponRepository.save(OrderCoupon.builder()
                    .order(order)
                    .coupon(couponApp.coupon())
                    .discountAmount(couponApp.discountAmount())
                    .build());
        }

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING)
                .note("Order placed (" + provider + ")")
                .changedByUserId(user.getId())
                .build());

        eventPublisher.publishEvent(new OrderEmailEvent(order.getId(), OrderEmailType.CONFIRMATION));

        return switch (provider) {
            case COD -> finalizeCodPlacement(order, grandTotal, currency, orderItems);
            case VNPAY -> finalizeVnpayPlacement(order, grandTotal, currency, orderItems, clientIp);
            case STRIPE -> finalizeStripePlacement(order, grandTotal, currency, orderItems);
            default -> throw new IllegalStateException("Unhandled provider: " + provider);
        };
    }

    private PlaceOrderResponse finalizeCodPlacement(Order order, BigDecimal grandTotal,
                                                    String currency, List<OrderItem> orderItems) {
        paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.COD)
                .amount(grandTotal)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .build());
        return PlaceOrderResponse.builder()
                .order(orderMapper.toDetailDto(order, orderItems))
                .build();
    }

    private PlaceOrderResponse finalizeVnpayPlacement(Order order, BigDecimal grandTotal, String currency,
                                                      List<OrderItem> orderItems, String clientIp) {
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.VNPAY)
                .providerTxnId(order.getOrderNumber())
                .amount(grandTotal)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .build());
        String redirectUrl = vnpayService.buildPaymentUrl(order.getOrderNumber(), grandTotal, clientIp);
        return PlaceOrderResponse.builder()
                .order(orderMapper.toDetailDto(order, orderItems))
                .redirectUrl(redirectUrl)
                .paymentRef(payment.getProviderTxnId())
                .build();
    }

    private PlaceOrderResponse finalizeStripePlacement(Order order, BigDecimal grandTotal,
                                                       String currency, List<OrderItem> orderItems) {
        FxQuote quote = fxService.quoteVndToUsd(grandTotal);
        long usdMinor = quote.convertedAmountInMinorUnits();

        StripeService.StripeSession session = stripeService.createCheckoutSession(
                order.getOrderNumber(), usdMinor, quote.convertedCurrency());

        Map<String, Object> snapshot = quote.toSnapshot();
        snapshot.put("checkoutSessionId", session.sessionId());

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .providerTxnId(session.sessionId())
                .amount(quote.convertedAmount())
                .currency(quote.convertedCurrency())
                .status(PaymentStatus.PENDING)
                .rawRequest(snapshot)
                .build());

        return PlaceOrderResponse.builder()
                .order(orderMapper.toDetailDto(order, orderItems))
                .redirectUrl(session.checkoutUrl())
                .paymentRef(payment.getProviderTxnId())
                .build();
    }

    @Override
    public PageResponse<OrderSummaryDto> listOrders(String email, Pageable pageable, String locale) {
        User user = loadUser(email);

        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Pageable safe = PageRequest.of(Math.max(pageable.getPageNumber(), 0), safeSize);

        Page<Order> page = orderRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), safe);
        return PageResponse.from(page, orderMapper.toSummaryDtos(page.getContent(), locale));
    }

    @Override
    public OrderDetailDto getOrder(String email, String orderNumber, String locale) {
        User user = loadUser(email);
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        return orderMapper.toDetailDto(order, items, locale);
    }

    @Override
    @Transactional
    public OrderDetailDto cancelOrder(String email, String orderNumber) {
        User user = loadUser(email);
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        List<OrderItem> items = cancelPendingOrder(order, "Cancelled by customer", user.getId());
        return orderMapper.toDetailDto(order, items);
    }

    @Override
    public List<Long> findStalePendingOrderIds() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(pendingTimeoutMinutes));
        return orderRepository.findStalePendingPayableOrderIds(
                OrderStatus.PENDING, cutoff, PaymentProvider.COD, PaymentStatus.CAPTURED);
    }

    @Override
    @Transactional
    public boolean autoCancelStaleOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        // May have been paid or cancelled since the candidate query.
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        cancelPendingOrder(order, "Auto-cancelled: payment not completed in time", null);
        return true;
    }

    private List<OrderItem> cancelPendingOrder(Order order, String note, Long changedByUserId) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());

        List<Long> variantIds = items.stream().map(oi -> oi.getVariant().getId()).distinct().toList();
        Map<Long, ProductVariant> variants = variantRepository
                .findAllByIdInWithProductForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (OrderItem oi : items) {
            ProductVariant v = variants.get(oi.getVariant().getId());
            if (v == null) {
                throw new IllegalStateException(
                        "Variant " + oi.getVariant().getId() + " not found while cancelling order " + order.getOrderNumber());
            }
            v.setStockQuantity(v.getStockQuantity() + oi.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        couponService.releaseForOrder(order.getId());

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note(note)
                .changedByUserId(changedByUserId)
                .build());

        eventPublisher.publishEvent(new OrderEmailEvent(order.getId(), OrderEmailType.CANCELLED));

        // payment enum has no CANCELLED; FAILED is the closest terminal state.
        paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).ifPresent(p -> {
            p.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(p);
        });

        return items;
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Address loadAddress(User user, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    private PaymentProvider parseProvider(String raw) {
        try {
            return PaymentProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid paymentMethod '" + raw + "'. Allowed: COD, VNPAY, STRIPE");
        }
    }

    private boolean isVariantAvailable(ProductVariant variant) {
        if (Boolean.FALSE.equals(variant.getIsActive())) return false;
        Product product = variant.getProduct();
        if (Boolean.FALSE.equals(product.getIsActive())) return false;
        return product.getDeletedAt() == null;
    }

    private int safeStock(ProductVariant variant) {
        return variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
    }

    private String formatVariantLabel(ProductVariant v) {
        return v.getSize() + " / " + v.getColor();
    }

    private record LineRequest(Long variantId, int quantity) {}
}
