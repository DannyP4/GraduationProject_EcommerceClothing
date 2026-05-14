package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderItemDto;
import com.uniform.store.dto.response.OrderStatusHistoryDto;
import com.uniform.store.dto.response.PaymentDto;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.OrderItem;
import com.uniform.store.entity.OrderStatusHistory;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.ProductImage;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.OrderItemRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.OrderStatusHistoryRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.repository.ProductImageRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.FxService;
import com.uniform.store.service.PaymentService;
import com.uniform.store.service.StripeService;
import com.uniform.store.service.VnpayService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final UserRepository userRepository;
    private final FxService fxService;
    private final VnpayService vnpayService;
    private final StripeService stripeService;

    @Override
    @Transactional
    public VnpayReturnResult handleVnpayReturn(Map<String, String> params) {
        VnpayService.VerifyResult verify = vnpayService.verifyReturn(params);

        if (verify.orderNumber() == null) {
            return new VnpayReturnResult(false, null, verify.message(), null);
        }

        Order order = orderRepository.findByOrderNumber(verify.orderNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", verify.orderNumber()));

        Payment payment = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc(verify.orderNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "providerTxnId", verify.orderNumber()));

        if (!verify.signatureValid()) {
            Sentry.captureMessage("vnpay.signature.invalid order=" + verify.orderNumber());
            log.warn("VNPAY signature mismatch for order {}", verify.orderNumber());
            return new VnpayReturnResult(false, verify.orderNumber(),
                    "Signature mismatch", buildDetailDto(order));
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED && order.getStatus() == OrderStatus.PAID) {
            return new VnpayReturnResult(true, verify.orderNumber(),
                    "Payment already confirmed", buildDetailDto(order));
        }

        Map<String, Object> rawResp = new LinkedHashMap<>(verify.rawParams());
        payment.setRawResponse(rawResp);

        if (verify.paymentSuccess()) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            statusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.PAID)
                    .note("VNPAY captured (bank txn " + verify.providerTxnId() + ")")
                    .build());
            return new VnpayReturnResult(true, verify.orderNumber(),
                    "Payment confirmed", buildDetailDto(order));
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        Sentry.captureMessage("vnpay.payment.failed order=" + verify.orderNumber()
                + " code=" + verify.responseCode());
        return new VnpayReturnResult(false, verify.orderNumber(),
                "Payment failed at gateway (code " + verify.responseCode() + ")",
                buildDetailDto(order));
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        StripeService.WebhookEvent event = stripeService.parseWebhook(payload, signatureHeader);

        if (!"checkout.session.completed".equals(event.type())) {
            return;
        }

        if (event.sessionId() == null) {
            throw new BadRequestException("Stripe webhook missing session id");
        }

        Payment payment = paymentRepository.findFirstByProviderTxnIdOrderByIdDesc(event.sessionId())
                .orElse(null);
        if (payment == null) {
            Sentry.captureMessage("stripe.webhook.payment_not_found session=" + event.sessionId());
            log.warn("Stripe webhook for unknown session {}", event.sessionId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return;
        }

        Map<String, Object> resp = payment.getRawResponse() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(payment.getRawResponse());
        resp.put("eventType", event.type());
        resp.put("paymentIntent", event.paymentIntentId());
        resp.put("stripePaymentStatus", event.status());
        resp.put("receivedAt", Instant.now().toString());

        if ("paid".equalsIgnoreCase(event.status())) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setPaidAt(Instant.now());
            if (event.paymentIntentId() != null) {
                resp.put("paymentIntentId", event.paymentIntentId());
            }
            payment.setRawResponse(resp);
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                statusHistoryRepository.save(OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.PAID)
                        .note("Stripe payment captured (session " + event.sessionId() + ")")
                        .build());
            }
        } else {
            payment.setRawResponse(resp);
            paymentRepository.save(payment);
            Sentry.captureMessage("stripe.session.unpaid session=" + event.sessionId()
                    + " status=" + event.status());
        }
    }

    @Override
    @Transactional
    public RetryResult retryPayment(String email, String orderNumber, String clientIp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be retried. Current: " + order.getStatus());
        }
        Payment latest = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", order.getId()));
        PaymentProvider provider = latest.getProvider();
        if (provider == PaymentProvider.COD) {
            throw new BadRequestException("COD orders cannot be retried — they are paid on delivery.");
        }
        if (latest.getStatus() == PaymentStatus.CAPTURED) {
            throw new BadRequestException("Payment is already captured.");
        }

        if (provider == PaymentProvider.VNPAY) {
            latest.setStatus(PaymentStatus.PENDING);
            latest.setRawResponse(null);
            paymentRepository.save(latest);
            String url = vnpayService.buildPaymentUrl(order.getOrderNumber(), order.getGrandTotal(), clientIp);
            return new RetryResult(provider.name(), url, latest.getProviderTxnId());
        }

        latest.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(latest);

        FxQuote quote = fxService.quoteVndToUsd(order.getGrandTotal());
        long usdMinor = quote.convertedAmountInMinorUnits();
        StripeService.StripeSession session = stripeService.createCheckoutSession(
                order.getOrderNumber(), usdMinor, quote.convertedCurrency());

        Map<String, Object> snapshot = quote.toSnapshot();
        snapshot.put("checkoutSessionId", session.sessionId());
        snapshot.put("retryOfPaymentId", latest.getId());

        Payment fresh = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .providerTxnId(session.sessionId())
                .amount(quote.convertedAmount())
                .currency(quote.convertedCurrency())
                .status(PaymentStatus.PENDING)
                .rawRequest(snapshot)
                .build());

        return new RetryResult(provider.name(), session.checkoutUrl(), fresh.getProviderTxnId());
    }

    private OrderDetailDto buildDetailDto(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
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

        final Map<Long, ProductVariant> fv = variantMap;
        final Map<Long, String> fi = primaryImages;

        List<OrderItemDto> itemDtos = items.stream().map(oi -> {
            ProductVariant v = fv.get(oi.getVariant().getId());
            String slug = v != null ? v.getProduct().getSlug() : null;
            String img = v != null ? fi.get(v.getProduct().getId()) : null;
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
