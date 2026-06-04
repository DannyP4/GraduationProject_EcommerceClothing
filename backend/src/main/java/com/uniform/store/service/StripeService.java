package com.uniform.store.service;

public interface StripeService {

    StripeSession createCheckoutSession(String orderNumber, long amountInMinorUnits, String currency);

    WebhookEvent parseWebhook(String payload, String signatureHeader);

    RefundResult refund(String paymentIntentId, long amountInMinorUnits);

    record StripeSession(String sessionId, String checkoutUrl) {}

    record WebhookEvent(String type, String sessionId, String paymentIntentId, String status) {}

    record RefundResult(String refundId, String status) {}
}
