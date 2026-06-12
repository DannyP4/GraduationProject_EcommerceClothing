package com.uniform.store.integration;

import com.uniform.store.entity.Order;
import com.uniform.store.entity.Payment;
import com.uniform.store.entity.User;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.PaymentProvider;
import com.uniform.store.enums.PaymentStatus;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentVnpayIntegrationTest extends BaseIntegrationTest {

    private static final String HASH_SECRET = "TEST_HASH_SECRET_FOR_VNPAY_HMAC_SHA512";

    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;

    User buyer;
    Order order;
    Payment payment;

    @BeforeEach
    void seedPendingVnpayOrder() {
        buyer = data.createCustomer("vnpay@uniform.test", "Test1234");
        order = orderRepository.save(Order.builder()
                .orderNumber("ORD-IT-VNP001")
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
                .provider(PaymentProvider.VNPAY)
                .providerTxnId("ORD-IT-VNP001")
                .amount(new BigDecimal("250000")).currency("VND")
                .status(PaymentStatus.PENDING).build());
    }

    @Test
    void verify_validSignatureAndSuccessCode_flipsOrderToPaid() throws Exception {
        Map<String, String> params = sampleReturnParams("ORD-IT-VNP001", "00", "00");
        signParams(params);

        mockMvc.perform(get("/payments/vnpay/verify").queryParams(toMultiValueMap(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        Order reloaded = orderRepository.findByOrderNumber("ORD-IT-VNP001").orElseThrow();
        Payment reloadedPayment = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc("ORD-IT-VNP001").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(reloadedPayment.getPaidAt()).isNotNull();
    }

    @Test
    void verify_replayAfterCaptured_returnsIdempotentSuccessWithoutDoubleUpdate() throws Exception {
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(Instant.parse("2026-05-14T10:00:00Z"));
        paymentRepository.save(payment);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        Map<String, String> params = sampleReturnParams("ORD-IT-VNP001", "00", "00");
        signParams(params);

        mockMvc.perform(get("/payments/vnpay/verify").queryParams(toMultiValueMap(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment already confirmed"));

        Payment reloadedPayment = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc("ORD-IT-VNP001").orElseThrow();
        assertThat(reloadedPayment.getPaidAt())
                .as("paidAt not overwritten on replay")
                .isEqualTo(Instant.parse("2026-05-14T10:00:00Z"));
    }

    @Test
    void verify_tamperedAmount_doesNotFlipOrder() throws Exception {
        Map<String, String> params = sampleReturnParams("ORD-IT-VNP001", "00", "00");
        signParams(params);
        params.put("vnp_Amount", "1");

        mockMvc.perform(get("/payments/vnpay/verify").queryParams(toMultiValueMap(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false));

        Order reloaded = orderRepository.findByOrderNumber("ORD-IT-VNP001").orElseThrow();
        Payment reloadedPayment = paymentRepository
                .findFirstByProviderTxnIdOrderByIdDesc("ORD-IT-VNP001").orElseThrow();
        assertThat(reloaded.getStatus()).as("order untouched").isEqualTo(OrderStatus.PENDING);
        assertThat(reloadedPayment.getStatus()).as("payment untouched").isEqualTo(PaymentStatus.PENDING);
    }

    private static Map<String, String> sampleReturnParams(String orderNumber, String code, String txnStatus) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_TmnCode", "TEST_TMN");
        p.put("vnp_Amount", "25000000");
        p.put("vnp_BankCode", "NCB");
        p.put("vnp_BankTranNo", "VNP14393107");
        p.put("vnp_CardType", "ATM");
        p.put("vnp_OrderInfo", "Payment for order " + orderNumber);
        p.put("vnp_PayDate", "20260514103000");
        p.put("vnp_ResponseCode", code);
        p.put("vnp_TransactionNo", "14393107");
        p.put("vnp_TransactionStatus", txnStatus);
        p.put("vnp_TxnRef", orderNumber);
        return p;
    }

    private static void signParams(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (signData.length() > 0) signData.append('&');
            signData.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII));
            signData.append('=');
            signData.append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        params.put("vnp_SecureHash", hmacSha512(HASH_SECRET, signData.toString()));
    }

    private static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static org.springframework.util.MultiValueMap<String, String> toMultiValueMap(Map<String, String> p) {
        org.springframework.util.LinkedMultiValueMap<String, String> m = new org.springframework.util.LinkedMultiValueMap<>();
        p.forEach(m::add);
        return m;
    }
}
