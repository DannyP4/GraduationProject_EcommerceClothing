package com.uniform.store.service.impl;

import com.uniform.store.config.VnpayProperties;
import com.uniform.store.service.VnpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayServiceImplTest {

    private static final String SECRET = "Cd8LVcvx95CgYgP2pFIyXbnkDqLViRmboRI7bpogWHXpEl0l5yXKCnELj0T84924rd3pRdvMBo80njbZ1r1jJi";

    private VnpayServiceImpl service;

    @BeforeEach
    void setUp() {
        VnpayProperties props = new VnpayProperties();
        props.setTmnCode("TEST_TMN");
        props.setHashSecret(SECRET);
        props.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        props.setReturnUrl("http://localhost:5173/payment/vnpay/return");
        props.setVersion("2.1.0");
        props.setCurrencyCode("VND");
        props.setLocale("vn");
        props.setCommand("pay");
        props.setExpireMinutes(15);
        service = new VnpayServiceImpl(props);
    }

    @Test
    void buildPaymentUrl_containsSecureHashAndAmountInMinorUnits() {
        String url = service.buildPaymentUrl("ORD-20260514-ABC123", new BigDecimal("250000"), "127.0.0.1");

        assertThat(url).as("starts with VNPAY gateway URL")
                .startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(url).as("VNPAY amount is in cents (250000 VND → 25000000)")
                .contains("vnp_Amount=25000000");
        assertThat(url).contains("vnp_TmnCode=TEST_TMN");
        assertThat(url).contains("vnp_TxnRef=ORD-20260514-ABC123");
        assertThat(url).as("URL must carry signature").contains("&vnp_SecureHash=");
    }

    @Test
    void verifyReturn_validSignatureAndSuccessCodes_marksPaymentSuccess() {
        Map<String, String> params = sampleReturnParams("00", "00");
        signAndInject(params);

        VnpayService.VerifyResult result = service.verifyReturn(params);

        assertThat(result.signatureValid()).as("HMAC matches").isTrue();
        assertThat(result.paymentSuccess()).as("code 00 + txn 00 = success").isTrue();
        assertThat(result.orderNumber()).isEqualTo("ORD-TEST-001");
    }

    @Test
    void verifyReturn_tamperedAmount_failsSignatureCheck() {
        Map<String, String> params = sampleReturnParams("00", "00");
        signAndInject(params);
        params.put("vnp_Amount", "1");

        VnpayService.VerifyResult result = service.verifyReturn(params);

        assertThat(result.signatureValid()).as("tampered field invalidates HMAC").isFalse();
        assertThat(result.paymentSuccess()).isFalse();
        assertThat(result.message()).isEqualTo("Signature mismatch");
    }

    @Test
    void verifyReturn_validSignatureButFailureCode_returnsPaymentFailed() {
        Map<String, String> params = sampleReturnParams("24", "02");
        signAndInject(params);

        VnpayService.VerifyResult result = service.verifyReturn(params);

        assertThat(result.signatureValid()).as("HMAC valid").isTrue();
        assertThat(result.paymentSuccess()).as("code 24 = customer cancelled at gateway").isFalse();
        assertThat(result.responseCode()).isEqualTo("24");
    }

    private static Map<String, String> sampleReturnParams(String responseCode, String txnStatus) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_TmnCode", "TEST_TMN");
        p.put("vnp_Amount", "25000000");
        p.put("vnp_BankCode", "NCB");
        p.put("vnp_BankTranNo", "VNP14393107");
        p.put("vnp_CardType", "ATM");
        p.put("vnp_OrderInfo", "Thanh toan don hang ORD-TEST-001");
        p.put("vnp_PayDate", "20260514103000");
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionNo", "14393107");
        p.put("vnp_TransactionStatus", txnStatus);
        p.put("vnp_TxnRef", "ORD-TEST-001");
        return p;
    }

    private static void signAndInject(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (signData.length() > 0) signData.append('&');
            signData.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII));
            signData.append('=');
            signData.append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        params.put("vnp_SecureHash", hmacSha512(SECRET, signData.toString()));
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
}
