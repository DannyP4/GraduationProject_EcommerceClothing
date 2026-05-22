package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AdminOrderFilter;
import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.dto.response.AdminOrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.OrderTransitions;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.OrderMapper;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.repository.spec.OrderSpecs;
import com.uniform.store.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    public PageResponse<AdminOrderSummaryDto> listOrders(AdminOrderFilter filter, Pageable pageable) {
        Specification<Order> spec = buildSpec(filter);
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return PageResponse.from(page, orderMapper.toAdminSummaryDtos(page.getContent()));
    }

    @Override
    public AdminOrderDetailDto getOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        return orderMapper.toAdminDetailDto(order, items);
    }

    @Override
    @Transactional
    public AdminOrderDetailDto transitionOrder(String orderNumber, OrderStatus targetStatus,
                                               String note, String actorEmail) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        OrderTransitions.assertCanTransition(order.getStatus(), targetStatus);

        User actor = loadActor(actorEmail);
        order.setStatus(targetStatus);
        orderRepository.save(order);

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(targetStatus)
                .note(buildTransitionNote(actorEmail, note))
                .changedByUserId(actor.getId())
                .build());

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        return orderMapper.toAdminDetailDto(order, items);
    }

    @Override
    @Transactional
    public AdminOrderDetailDto cancelOrder(String orderNumber, String reason, String actorEmail) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (!OrderTransitions.isCancellableByAdmin(order.getStatus())) {
            throw new BadRequestException(
                    "Cannot cancel order in status " + order.getStatus()
                            + ". Allowed: PENDING, PAID, PROCESSING.");
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
                        "Variant " + oi.getVariant().getId() + " missing while admin cancels " + orderNumber);
            }
            v.setStockQuantity(v.getStockQuantity() + oi.getQuantity());
        }

        User actor = loadActor(actorEmail);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note(buildCancelNote(actorEmail, reason))
                .changedByUserId(actor.getId())
                .build());

        return orderMapper.toAdminDetailDto(order, items);
    }

    private Specification<Order> buildSpec(AdminOrderFilter filter) {
        Specification<Order> spec = null;
        if (filter == null) return null;
        spec = and(spec, OrderSpecs.hasStatus(filter.getStatus()));
        spec = and(spec, OrderSpecs.placedAtOrAfter(filter.getPlacedFrom()));
        spec = and(spec, OrderSpecs.placedBefore(filter.getPlacedTo()));
        spec = and(spec, OrderSpecs.matchesSearch(filter.getSearch()));
        return spec;
    }

    private Specification<Order> and(Specification<Order> a, Specification<Order> b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.and(b);
    }

    private User loadActor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String buildTransitionNote(String actorEmail, String userNote) {
        String base = "Transitioned by " + actorEmail;
        return userNote == null || userNote.isBlank() ? base : base + ": " + userNote;
    }

    private String buildCancelNote(String actorEmail, String reason) {
        String base = "Admin cancel by " + actorEmail;
        return reason == null || reason.isBlank() ? base : base + ": " + reason;
    }
}
