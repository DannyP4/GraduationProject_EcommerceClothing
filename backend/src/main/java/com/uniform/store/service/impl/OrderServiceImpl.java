package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderItemDto;
import com.uniform.store.dto.response.OrderStatusHistoryDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.PaymentDto;
import com.uniform.store.dto.response.PlaceOrderResponse;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Cart;
import com.uniform.store.entity.CartItem;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.FxService;
import com.uniform.store.service.OrderService;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private final ProductImageRepository imageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final FxService fxService;
    private final VnpayService vnpayService;
    private final StripeService stripeService;

    @Override
    @Transactional
    public PlaceOrderResponse placeOrder(String email, PlaceOrderRequest req, String clientIp) {
        PaymentProvider provider = parseProvider(req.getPaymentMethod());
        if (provider == PaymentProvider.BANK_TRANSFER) {
            throw new BadRequestException("BANK_TRANSFER is not supported yet.");
        }

        User user = loadUser(email);
        Address address = addressRepository.findByIdAndUserId(req.getAddressId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", req.getAddressId()));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<Long> variantIds = items.stream().map(ci -> ci.getVariant().getId()).toList();
        // pessimistic lock against concurrent placeOrder/cancelOrder on the same variant.
        Map<Long, ProductVariant> variants = variantRepository
                .findAllByIdInWithProductForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<String> blocked = new ArrayList<>();
        for (CartItem ci : items) {
            ProductVariant v = variants.get(ci.getVariant().getId());
            if (v == null || !isVariantAvailable(v)) {
                blocked.add(skuOrId(v, ci));
                continue;
            }
            int stock = safeStock(v);
            if (stock < ci.getQuantity()) {
                blocked.add(v.getSku() + " (need " + ci.getQuantity() + ", left " + stock + ")");
            }
        }
        if (!blocked.isEmpty()) {
            throw new BadRequestException(
                    "Items unavailable: " + String.join(", ", blocked) + ". Please update your cart.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        List<OrderItem> orderItems = new ArrayList<>(items.size());

        for (CartItem ci : items) {
            ProductVariant v = variants.get(ci.getVariant().getId());
            Product p = v.getProduct();
            BigDecimal unitPrice = v.getPriceOverride() != null ? v.getPriceOverride() : p.getBasePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            currency = p.getCurrency();

            orderItems.add(OrderItem.builder()
                    .variant(v)
                    .productName(p.getName())
                    .variantLabel(formatVariantLabel(v))
                    .sku(v.getSku())
                    .unitPrice(unitPrice)
                    .quantity(ci.getQuantity())
                    .lineTotal(lineTotal)
                    .build());

            v.setStockQuantity(v.getStockQuantity() - ci.getQuantity());
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal shippingCost = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
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
                .notes(req.getNotes())
                .placedAt(Instant.now())
                .build();
        orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            orderItemRepository.save(oi);
        }

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING)
                .note("Order placed (" + provider + ")")
                .changedByUserId(user.getId())
                .build());

        cartItemRepository.deleteAllByCartId(cart.getId());

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
                .order(buildDetailDto(order, orderItems))
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
                .order(buildDetailDto(order, orderItems))
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
                .order(buildDetailDto(order, orderItems))
                .redirectUrl(session.checkoutUrl())
                .paymentRef(payment.getProviderTxnId())
                .build();
    }

    @Override
    public PageResponse<OrderSummaryDto> listOrders(String email, Pageable pageable) {
        User user = loadUser(email);

        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Pageable safe = PageRequest.of(Math.max(pageable.getPageNumber(), 0), safeSize);

        Page<Order> page = orderRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), safe);
        List<Order> orders = page.getContent();
        if (orders.isEmpty()) {
            return PageResponse.from(page, List.of());
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdInOrderByOrderIdAscIdAsc(orderIds);

        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(
                        oi -> oi.getOrder().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Long> firstVariantIds = itemsByOrder.values().stream()
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0).getVariant().getId())
                .distinct()
                .toList();

        Map<Long, ProductVariant> variantMap = firstVariantIds.isEmpty()
                ? Map.of()
                : variantRepository.findAllByIdInWithProduct(firstVariantIds).stream()
                        .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<Long> productIds = variantMap.values().stream()
                .map(v -> v.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, String> primaryImages = new LinkedHashMap<>();
        if (!productIds.isEmpty()) {
            for (ProductImage img : imageRepository.findThumbnailCandidatesByProductIds(productIds)) {
                primaryImages.putIfAbsent(img.getProduct().getId(), img.getUrl());
            }
        }

        List<OrderSummaryDto> dtos = orders.stream().map(o -> {
            List<OrderItem> orderItemList = itemsByOrder.getOrDefault(o.getId(), List.of());
            OrderItem first = orderItemList.isEmpty() ? null : orderItemList.get(0);

            String thumbUrl = null;
            if (first != null) {
                ProductVariant v = variantMap.get(first.getVariant().getId());
                if (v != null) {
                    thumbUrl = primaryImages.get(v.getProduct().getId());
                }
            }
            int totalQty = orderItemList.stream().mapToInt(OrderItem::getQuantity).sum();

            return OrderSummaryDto.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus())
                    .itemCount(totalQty)
                    .grandTotal(o.getGrandTotal())
                    .currency(o.getCurrency())
                    .placedAt(o.getPlacedAt())
                    .firstItemName(first != null ? first.getProductName() : null)
                    .thumbnailUrl(thumbUrl)
                    .build();
        }).toList();

        return PageResponse.from(page, dtos);
    }

    @Override
    public OrderDetailDto getOrder(String email, String orderNumber) {
        User user = loadUser(email);
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        return buildDetailDto(order, items);
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

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());

        List<Long> variantIds = items.stream().map(oi -> oi.getVariant().getId()).distinct().toList();
        Map<Long, ProductVariant> variants = variantRepository
                .findAllByIdInWithProductForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (OrderItem oi : items) {
            ProductVariant v = variants.get(oi.getVariant().getId());
            if (v == null) {
                throw new IllegalStateException(
                        "Variant " + oi.getVariant().getId() + " not found while cancelling order " + orderNumber);
            }
            v.setStockQuantity(v.getStockQuantity() + oi.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note("Cancelled by customer")
                .changedByUserId(user.getId())
                .build());

        // payment enum has no CANCELLED; FAILED is the closest terminal state.
        paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).ifPresent(p -> {
            p.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(p);
        });

        return buildDetailDto(order, items);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
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

    private String skuOrId(ProductVariant v, CartItem ci) {
        return v != null ? v.getSku() : "variant#" + ci.getVariant().getId();
    }

    private String formatVariantLabel(ProductVariant v) {
        return v.getSize() + " / " + v.getColor();
    }

    private OrderDetailDto buildDetailDto(Order order, List<OrderItem> items) {
        Map<Long, ProductVariant> variantMap = Map.of();
        Map<Long, String> primaryImages = Map.of();

        if (!items.isEmpty()) {
            List<Long> variantIds = items.stream().map(oi -> oi.getVariant().getId()).distinct().toList();
            variantMap = variantRepository.findAllByIdInWithProduct(variantIds).stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

            List<Long> productIds = variantMap.values().stream()
                    .map(v -> v.getProduct().getId())
                    .distinct()
                    .toList();

            if (!productIds.isEmpty()) {
                Map<Long, String> imgs = new LinkedHashMap<>();
                for (ProductImage img : imageRepository.findThumbnailCandidatesByProductIds(productIds)) {
                    imgs.putIfAbsent(img.getProduct().getId(), img.getUrl());
                }
                primaryImages = imgs;
            }
        }

        final Map<Long, ProductVariant> finalVariantMap = variantMap;
        final Map<Long, String> finalPrimaryImages = primaryImages;

        List<OrderItemDto> itemDtos = items.stream().map(oi -> {
            ProductVariant v = finalVariantMap.get(oi.getVariant().getId());
            String slug = null;
            String img = null;
            if (v != null) {
                slug = v.getProduct().getSlug();
                img = finalPrimaryImages.get(v.getProduct().getId());
            }
            return OrderItemDto.builder()
                    .id(oi.getId())
                    .variantId(oi.getVariant().getId())
                    .productName(oi.getProductName())
                    .variantLabel(oi.getVariantLabel())
                    .sku(oi.getSku())
                    .unitPrice(oi.getUnitPrice())
                    .quantity(oi.getQuantity())
                    .lineTotal(oi.getLineTotal())
                    .productSlug(slug)
                    .imageUrl(img)
                    .build();
        }).toList();

        List<OrderStatusHistoryDto> historyDtos = statusHistoryRepository
                .findByOrderIdOrderByChangedAtAscIdAsc(order.getId()).stream()
                .map(h -> OrderStatusHistoryDto.builder()
                        .id(h.getId())
                        .status(h.getStatus())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        Optional<Payment> latestPayment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId());
        PaymentDto paymentDto = latestPayment.map(p -> PaymentDto.builder()
                .id(p.getId())
                .provider(p.getProvider())
                .status(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paidAt(p.getPaidAt())
                .build()).orElse(null);

        return OrderDetailDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .items(itemDtos)
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .shippingCost(order.getShippingCost())
                .taxTotal(order.getTaxTotal())
                .grandTotal(order.getGrandTotal())
                .currency(order.getCurrency())
                .shippingRecipient(order.getShippingRecipient())
                .shippingPhone(order.getShippingPhone())
                .shippingLine1(order.getShippingLine1())
                .shippingWard(order.getShippingWard())
                .shippingDistrict(order.getShippingDistrict())
                .shippingCity(order.getShippingCity())
                .shippingCountry(order.getShippingCountry())
                .shippingPostalCode(order.getShippingPostalCode())
                .notes(order.getNotes())
                .placedAt(order.getPlacedAt())
                .statusHistory(historyDtos)
                .payment(paymentDto)
                .cancellable(order.getStatus() == OrderStatus.PENDING)
                .build();
    }
}
