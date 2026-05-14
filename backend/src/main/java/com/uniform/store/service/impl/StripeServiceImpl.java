package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;


import com.uniform.store.config.StripeProperties;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.StripeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements StripeService {

    private final StripeProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        if (props.getSecretKey() != null && !props.getSecretKey().isBlank()) {
            Stripe.apiKey = props.getSecretKey();
        }
    }

    @Override
    public StripeSession createCheckoutSession(String orderNumber, long amountInMinorUnits, String currency) {
        String successUrl = props.getSuccessUrl().replace("{ORDER_NUMBER}", orderNumber);
        String cancelUrl = props.getCancelUrl().replace("{ORDER_NUMBER}", orderNumber);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(orderNumber)
                .putMetadata("orderNumber", orderNumber)
                // force USD-only display; otherwise Stripe shows a local-currency option with their markup (VND).
                .setAdaptivePricing(SessionCreateParams.AdaptivePricing.builder()
                        .setEnabled(false)
                        .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency.toLowerCase())
                                .setUnitAmount(amountInMinorUnits)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("UNIFORM Order " + orderNumber)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params);
            return new StripeSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new BadRequestException("Stripe Checkout session creation failed: " + e.getMessage());
        }
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, props.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Invalid Stripe webhook signature");
        }
        String sessionId = null;
        String paymentIntent = null;
        String paymentStatus = null;
        try {
            JsonNode obj = objectMapper.readTree(payload)
                    .path("data").path("object");
            sessionId = obj.path("id").asText(null);
            paymentIntent = obj.path("payment_intent").asText(null);
            paymentStatus = obj.path("payment_status").asText(null);
        } catch (Exception ignored) {
        }
        return new WebhookEvent(event.getType(), sessionId, paymentIntent, paymentStatus);
    }
}
