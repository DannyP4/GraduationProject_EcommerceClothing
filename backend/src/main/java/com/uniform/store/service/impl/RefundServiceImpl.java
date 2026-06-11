package com.uniform.store.service.impl;

import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.OrderTransitions;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.event.OrderEmailEvent;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.mapper.OrderMapper;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.RefundService;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RefundServiceImpl implements RefundService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final StripeService stripeService;
    private final VnpayService vnpayService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AdminOrderDetailDto refundOrder(String orderNumber, String reason, String actorEmail) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (!OrderTransitions.isRefundableByAdmin(order.getStatus())) {
            throw new BadRequestException(
                    "Cannot refund order in status " + order.getStatus()
                            + ". Allowed: PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED.");
        }

        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new BadRequestException("No payment found for order " + orderNumber));
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new BadRequestException(
                    "Only captured payments can be refunded. Payment status: " + payment.getStatus()
                            + ". Use cancel for unpaid orders.");
        }

        String refundRef = executeGatewayRefund(order, payment);

        Map<String, Object> resp = payment.getRawResponse() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(payment.getRawResponse());
        resp.put("refundId", refundRef);
        resp.put("refundedAt", Instant.now().toString());
        resp.put("refundedBy", actorEmail);
        resp.put("refundAmount", payment.getAmount().toPlainString());
        resp.put("refundCurrency", payment.getCurrency());
        payment.setRawResponse(resp);
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PROCESSING) {
            restoreStock(items, orderNumber);
        }

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));

        OrderStatus historyStatus = order.getStatus();
        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
            historyStatus = OrderStatus.REFUNDED;
        }

        statusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(historyStatus)
                .note(buildRefundNote(actorEmail, reason, payment.getProvider(), refundRef))
                .changedByUserId(actor.getId())
                .build());

        eventPublisher.publishEvent(new OrderEmailEvent(order.getId(), OrderEmailType.REFUNDED));

        return orderMapper.toAdminDetailDto(order, items);
    }

    private String executeGatewayRefund(Order order, Payment payment) {
        PaymentProvider provider = payment.getProvider();
        if (provider == PaymentProvider.STRIPE) {
            String paymentIntentId = stripePaymentIntentId(payment);
            if (paymentIntentId == null) {
                throw new BadRequestException(
                        "Stripe payment intent not found on payment " + payment.getId());
            }
            long minor = payment.getAmount().setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2).longValueExact();
            return stripeService.refund(paymentIntentId, minor).refundId();
        }
        if (provider == PaymentProvider.VNPAY) {
            return vnpayService.refund(
                    order.getOrderNumber(), payment.getAmount(), payment.getProviderTxnId()).refundId();
        }
        return "OFFLINE-" + provider.name();
    }

    private String stripePaymentIntentId(Payment payment) {
        Map<String, Object> resp = payment.getRawResponse();
        if (resp == null) return null;
        Object pi = resp.get("paymentIntentId");
        if (pi == null) pi = resp.get("paymentIntent");
        return pi == null ? null : pi.toString();
    }

    private void restoreStock(List<OrderItem> items, String orderNumber) {
        List<Long> variantIds = items.stream().map(oi -> oi.getVariant().getId()).distinct().toList();
        Map<Long, ProductVariant> variants = variantRepository
                .findAllByIdInWithProductForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        for (OrderItem oi : items) {
            ProductVariant v = variants.get(oi.getVariant().getId());
            if (v == null) {
                throw new IllegalStateException(
                        "Variant " + oi.getVariant().getId() + " missing while refunding " + orderNumber);
            }
            v.setStockQuantity(v.getStockQuantity() + oi.getQuantity());
        }
    }

    private String buildRefundNote(String actorEmail, String reason, PaymentProvider provider, String refundRef) {
        String base = "Refunded by " + actorEmail + " via " + provider + " (ref " + refundRef + ")";
        return reason == null || reason.isBlank() ? base : base + ": " + reason;
    }
}
