package com.uniform.store.service;

import java.math.BigDecimal;
import java.util.Map;

public interface VnpayService {

    String buildPaymentUrl(String orderNumber, BigDecimal vndAmount, String clientIp);

    VerifyResult verifyReturn(Map<String, String> params);

    RefundResult refund(String orderNumber, BigDecimal vndAmount, String originalTxnId);

    record RefundResult(String refundId, String status, Map<String, Object> raw) {}

    record VerifyResult(
            boolean signatureValid,
            boolean paymentSuccess,
            String orderNumber,
            String responseCode,
            String transactionStatus,
            String providerTxnId,
            String message,
            Map<String, String> rawParams) {}
}
