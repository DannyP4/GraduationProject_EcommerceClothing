package com.uniform.store.service;

import com.uniform.store.dto.response.OrderDetailDto;

import java.util.Map;

public interface PaymentService {

    VnpayReturnResult handleVnpayReturn(Map<String, String> params);

    void handleStripeWebhook(String payload, String signatureHeader);

    RetryResult retryPayment(String email, String orderNumber, String clientIp);

    record VnpayReturnResult(
            boolean success,
            String orderNumber,
            String message,
            OrderDetailDto order) {}

    record RetryResult(String provider, String redirectUrl, String paymentRef) {}
}
