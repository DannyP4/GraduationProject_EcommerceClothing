package com.uniform.store.integration;

import com.uniform.store.entity.Order;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import com.uniform.store.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentStripeWebhookIntegrationTest extends BaseIntegrationTest {

    @MockBean StripeService stripeService;

    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;

    User buyer;
    Order order;
    Payment payment;

    @BeforeEach
    void seedPendingStripeOrder() {
        buyer = data.createCustomer("stripe@uniform.test", "Test1234");
        order = orderRepository.save(Order.builder()
                .orderNumber("ORD-IT-STR001")
                .user(buyer)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("250000"))
                .grandTotal(new BigDecimal("250000"))
                .currency("VND")
                .shippingRecipient("Buyer").shippingPhone("0912345678")
                .shippingLine1("123 Main").shippingDistrict("Q1").shippingCity("HCM")
                .shippingCountry("VN")
                .placedAt(Instant.now())
                .build());
        payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .providerTxnId("cs_test_it_001")
                .amount(new BigDecimal("9.63")).currency("USD")
                .status(PaymentStatus.PENDING).build());
    }

    @Test
    void webhook_checkoutSessionCompletedPaid_flipsOrderAndPayment() throws Exception {
        when(stripeService.parseWebhook(any(), any())).thenReturn(
                new StripeService.WebhookEvent(
                        "checkout.session.completed", "cs_test_it_001", "pi_test_xyz", "paid"));

        mockMvc.perform(post("/payments/stripe/webhook")
                        .header("Stripe-Signature", "t=1,v1=dummy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        Order reloaded = orderRepository.findByOrderNumber("ORD-IT-STR001").orElseThrow();
        Payment reloadedPayment = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc("cs_test_it_001").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(reloadedPayment.getPaidAt()).isNotNull();
    }

    @Test
    void webhook_replayAfterCaptured_doesNotDoubleUpdate() throws Exception {
        Instant originalPaidAt = Instant.parse("2026-05-14T10:00:00Z");
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(originalPaidAt);
        paymentRepository.save(payment);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        when(stripeService.parseWebhook(any(), any())).thenReturn(
                new StripeService.WebhookEvent(
                        "checkout.session.completed", "cs_test_it_001", "pi_test_xyz", "paid"));

        mockMvc.perform(post("/payments/stripe/webhook")
                        .header("Stripe-Signature", "t=1,v1=dummy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        Payment reloaded = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc("cs_test_it_001").orElseThrow();
        assertThat(reloaded.getPaidAt())
                .as("idempotent: paidAt not rewritten").isEqualTo(originalPaidAt);
    }

    @Test
    void webhook_invalidSignature_returns400AndDoesNotMutate() throws Exception {
        when(stripeService.parseWebhook(any(), any()))
                .thenThrow(new BadRequestException("Invalid Stripe webhook signature"));

        mockMvc.perform(post("/payments/stripe/webhook")
                        .header("Stripe-Signature", "tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Order reloaded = orderRepository.findByOrderNumber("ORD-IT-STR001").orElseThrow();
        assertThat(reloaded.getStatus()).as("order untouched").isEqualTo(OrderStatus.PENDING);
    }
}
