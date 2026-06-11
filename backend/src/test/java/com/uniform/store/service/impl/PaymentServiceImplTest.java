package com.uniform.store.service.impl;

import com.uniform.store.entity.Order;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.enums.UserStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock OrderStatusHistoryRepository statusHistoryRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductImageRepository imageRepository;
    @Mock UserRepository userRepository;
    @Mock FxService fxService;
    @Mock VnpayService vnpayService;
    @Mock StripeService stripeService;
    @Mock ApplicationEventPublisher eventPublisher;

    PaymentServiceImpl paymentService;

    User user;
    Order vnpayOrder;
    Order stripeOrder;
    Payment vnpayPayment;
    Payment stripePayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                orderRepository, orderItemRepository, statusHistoryRepository, paymentRepository,
                variantRepository, imageRepository, userRepository,
                fxService, vnpayService, stripeService, eventPublisher);

        user = User.builder()
                .email("buyer@uniform.test")
                .passwordHash("hash")
                .fullName("Buyer")
                .preferredLocale("vi")
                .role(Role.builder().name("customer").displayName("Customer").build())
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        vnpayOrder = baseOrder("ORD-VNP-001", OrderStatus.PENDING);
        vnpayPayment = Payment.builder()
                .order(vnpayOrder).provider(PaymentProvider.VNPAY)
                .providerTxnId("ORD-VNP-001")
                .amount(new BigDecimal("250000")).currency("VND")
                .status(PaymentStatus.PENDING).build();
        vnpayPayment.setId(500L);

        stripeOrder = baseOrder("ORD-STR-001", OrderStatus.PENDING);
        stripePayment = Payment.builder()
                .order(stripeOrder).provider(PaymentProvider.STRIPE)
                .providerTxnId("cs_test_abc")
                .amount(new BigDecimal("9.63")).currency("USD")
                .status(PaymentStatus.PENDING).build();
        stripePayment.setId(501L);
    }

    @Test
    void handleVnpayReturn_validSignatureAndSuccess_flipsOrderToPaidAndPaymentToCaptured() {
        VnpayService.VerifyResult vr = new VnpayService.VerifyResult(
                true, true, "ORD-VNP-001", "00", "00", "14393107",
                "Payment confirmed", Map.of("vnp_TransactionNo", "14393107"));
        when(vnpayService.verifyReturn(any())).thenReturn(vr);
        when(orderRepository.findByOrderNumber("ORD-VNP-001")).thenReturn(Optional.of(vnpayOrder));
        when(paymentRepository.findFirstByProviderTxnIdOrderByIdDesc("ORD-VNP-001"))
                .thenReturn(Optional.of(vnpayPayment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(statusHistoryRepository.findByOrderIdOrderByChangedAtAscIdAsc(anyLong())).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(anyLong())).thenReturn(Optional.of(vnpayPayment));

        PaymentService.VnpayReturnResult result = paymentService.handleVnpayReturn(new HashMap<>());

        assertThat(result.success()).as("flipped to PAID").isTrue();
        assertThat(vnpayOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(vnpayPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(vnpayPayment.getPaidAt()).as("paidAt timestamped").isNotNull();
    }

    @Test
    void handleVnpayReturn_replayWhenAlreadyCaptured_returnsIdempotentSuccess() {
        vnpayOrder.setStatus(OrderStatus.PAID);
        vnpayPayment.setStatus(PaymentStatus.CAPTURED);
        vnpayPayment.setPaidAt(Instant.parse("2026-05-14T10:00:00Z"));

        VnpayService.VerifyResult vr = new VnpayService.VerifyResult(
                true, true, "ORD-VNP-001", "00", "00", "14393107",
                "Payment confirmed", Map.of());
        when(vnpayService.verifyReturn(any())).thenReturn(vr);
        when(orderRepository.findByOrderNumber("ORD-VNP-001")).thenReturn(Optional.of(vnpayOrder));
        when(paymentRepository.findFirstByProviderTxnIdOrderByIdDesc("ORD-VNP-001"))
                .thenReturn(Optional.of(vnpayPayment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(statusHistoryRepository.findByOrderIdOrderByChangedAtAscIdAsc(anyLong())).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(anyLong())).thenReturn(Optional.of(vnpayPayment));

        PaymentService.VnpayReturnResult result = paymentService.handleVnpayReturn(new HashMap<>());

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Payment already confirmed");
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void handleVnpayReturn_invalidSignature_returnsFailureAndDoesNotMutate() {
        VnpayService.VerifyResult vr = new VnpayService.VerifyResult(
                false, false, "ORD-VNP-001", "00", "00", null,
                "Signature mismatch", Map.of());
        when(vnpayService.verifyReturn(any())).thenReturn(vr);
        when(orderRepository.findByOrderNumber("ORD-VNP-001")).thenReturn(Optional.of(vnpayOrder));
        when(paymentRepository.findFirstByProviderTxnIdOrderByIdDesc("ORD-VNP-001"))
                .thenReturn(Optional.of(vnpayPayment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(statusHistoryRepository.findByOrderIdOrderByChangedAtAscIdAsc(anyLong())).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(anyLong())).thenReturn(Optional.of(vnpayPayment));

        PaymentService.VnpayReturnResult result = paymentService.handleVnpayReturn(new HashMap<>());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Signature mismatch");
        assertThat(vnpayOrder.getStatus()).as("order untouched").isEqualTo(OrderStatus.PENDING);
        assertThat(vnpayPayment.getStatus()).as("payment untouched").isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void handleVnpayReturn_gatewayCode24_marksPaymentFailed() {
        VnpayService.VerifyResult vr = new VnpayService.VerifyResult(
                true, false, "ORD-VNP-001", "24", "02", "14393107",
                "Payment failed at gateway", Map.of("vnp_ResponseCode", "24"));
        when(vnpayService.verifyReturn(any())).thenReturn(vr);
        when(orderRepository.findByOrderNumber("ORD-VNP-001")).thenReturn(Optional.of(vnpayOrder));
        when(paymentRepository.findFirstByProviderTxnIdOrderByIdDesc("ORD-VNP-001"))
                .thenReturn(Optional.of(vnpayPayment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(statusHistoryRepository.findByOrderIdOrderByChangedAtAscIdAsc(anyLong())).thenReturn(List.of());
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(anyLong())).thenReturn(Optional.of(vnpayPayment));

        PaymentService.VnpayReturnResult result = paymentService.handleVnpayReturn(new HashMap<>());

        assertThat(result.success()).isFalse();
        assertThat(vnpayPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(vnpayOrder.getStatus()).as("order stays PENDING (cancellable)").isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void handleStripeWebhook_checkoutSessionCompletedPaid_flipsOrderAndPayment() {
        StripeService.WebhookEvent event = new StripeService.WebhookEvent(
                "checkout.session.completed", "cs_test_abc", "pi_test_xyz", "paid");
        when(stripeService.parseWebhook(any(), any())).thenReturn(event);
        when(paymentRepository.findFirstByProviderTxnIdOrderByIdDesc("cs_test_abc"))
                .thenReturn(Optional.of(stripePayment));

        paymentService.handleStripeWebhook("{}", "sig");

        assertThat(stripePayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(stripePayment.getPaidAt()).isNotNull();
        assertThat(stripeOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(statusHistoryRepository).save(any());
    }

    private Order baseOrder(String orderNumber, OrderStatus status) {
        Order o = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(status)
                .subtotal(new BigDecimal("250000"))
                .grandTotal(new BigDecimal("250000"))
                .currency("VND")
                .shippingRecipient("Buyer").shippingPhone("0912345678")
                .shippingLine1("123 Main").shippingDistrict("Q1").shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(Instant.now())
                .build();
        o.setId(orderNumber.hashCode() & 0xFFFFL);
        return o;
    }
}
