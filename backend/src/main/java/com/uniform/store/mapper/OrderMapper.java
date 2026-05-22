package com.uniform.store.mapper;

import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.dto.response.AdminOrderSummaryDto;
import com.uniform.store.dto.response.CustomerInfoDto;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderItemDto;
import com.uniform.store.dto.response.OrderStatusHistoryDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PaymentDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.OrderTransitions;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;

    public OrderDetailDto toDetailDto(Order order, List<OrderItem> items) {
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

    public List<OrderSummaryDto> toSummaryDtos(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        SummaryLookup look = buildSummaryLookup(orders);

        return orders.stream().map(o -> {
            List<OrderItem> itemList = look.itemsByOrder.getOrDefault(o.getId(), List.of());
            OrderItem first = itemList.isEmpty() ? null : itemList.get(0);

            String thumb = thumbnailFor(first, look);
            int totalQty = itemList.stream().mapToInt(OrderItem::getQuantity).sum();

            return OrderSummaryDto.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus())
                    .itemCount(totalQty)
                    .grandTotal(o.getGrandTotal())
                    .currency(o.getCurrency())
                    .placedAt(o.getPlacedAt())
                    .firstItemName(first != null ? first.getProductName() : null)
                    .thumbnailUrl(thumb)
                    .build();
        }).toList();
    }

    public List<AdminOrderSummaryDto> toAdminSummaryDtos(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        SummaryLookup look = buildSummaryLookup(orders);

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Payment> latestPayments = paymentRepository
                .findByOrderIdInOrderByOrderIdAscIdDesc(orderIds).stream()
                .collect(Collectors.toMap(
                        p -> p.getOrder().getId(),
                        Function.identity(),
                        (a, b) -> a.getId() > b.getId() ? a : b));

        return orders.stream().map(o -> {
            List<OrderItem> itemList = look.itemsByOrder.getOrDefault(o.getId(), List.of());
            OrderItem first = itemList.isEmpty() ? null : itemList.get(0);

            String thumb = thumbnailFor(first, look);
            int totalQty = itemList.stream().mapToInt(OrderItem::getQuantity).sum();

            Payment pay = latestPayments.get(o.getId());

            return AdminOrderSummaryDto.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus())
                    .itemCount(totalQty)
                    .grandTotal(o.getGrandTotal())
                    .currency(o.getCurrency())
                    .placedAt(o.getPlacedAt())
                    .firstItemName(first != null ? first.getProductName() : null)
                    .thumbnailUrl(thumb)
                    .customer(toCustomerInfo(o.getUser()))
                    .paymentProvider(pay != null ? pay.getProvider() : null)
                    .paymentStatus(pay != null ? pay.getStatus() : null)
                    .build();
        }).toList();
    }

    public AdminOrderDetailDto toAdminDetailDto(Order order, List<OrderItem> items) {
        OrderDetailDto base = toDetailDto(order, items);
        boolean requiresRefund = order.getStatus() == OrderStatus.CANCELLED
                && base.getPayment() != null
                && base.getPayment().getStatus() != null
                && base.getPayment().getStatus().name().equals("CAPTURED");

        return AdminOrderDetailDto.builder()
                .order(base)
                .customer(toCustomerInfo(order.getUser()))
                .allowedTransitions(OrderTransitions.allowedFrom(order.getStatus()))
                .cancellableByAdmin(OrderTransitions.isCancellableByAdmin(order.getStatus()))
                .requiresRefund(requiresRefund)
                .build();
    }

    public CustomerInfoDto toCustomerInfo(User user) {
        if (user == null) return null;
        return CustomerInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .build();
    }

    private SummaryLookup buildSummaryLookup(List<Order> orders) {
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
        return new SummaryLookup(itemsByOrder, variantMap, primaryImages);
    }

    private String thumbnailFor(OrderItem first, SummaryLookup look) {
        if (first == null) return null;
        ProductVariant v = look.variantMap.get(first.getVariant().getId());
        return v == null ? null : look.primaryImages.get(v.getProduct().getId());
    }

    private record SummaryLookup(
            Map<Long, List<OrderItem>> itemsByOrder,
            Map<Long, ProductVariant> variantMap,
            Map<Long, String> primaryImages
    ) {}
}
