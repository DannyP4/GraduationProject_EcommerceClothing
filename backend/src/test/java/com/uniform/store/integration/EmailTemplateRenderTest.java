package com.uniform.store.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRenderTest extends BaseIntegrationTest {

    @Autowired TemplateEngine templateEngine;

    private static final List<String> TEMPLATES = List.of(
            "email/order-confirmation",
            "email/payment-received",
            "email/order-shipped",
            "email/order-delivered",
            "email/order-cancelled",
            "email/order-refunded");

    private Context sampleContext() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("brandName", "Vesta");
        vars.put("recipientName", "Long Pham");
        vars.put("orderNumber", "VST-RENDER-1");
        vars.put("statusLabel", "Pending");
        vars.put("placedAt", "11 Jun 2026 at 14:30");
        vars.put("items", List.of(Map.of(
                "name", "Turtle Check Shirt", "variant", "M / Navy", "sku", "SKU-1",
                "quantity", 2, "unitPrice", "250,000", "lineTotal", "500,000")));
        vars.put("subtotal", "500,000");
        vars.put("discountTotal", "20,000");
        vars.put("hasDiscount", true);
        vars.put("shippingCost", "30,000");
        vars.put("grandTotal", "510,000");
        vars.put("paymentMethod", "Cash on Delivery");
        vars.put("shippingRecipient", "Long Pham");
        vars.put("shippingPhone", "0900000000");
        vars.put("shippingAddress", "1 Dai Co Viet, Hai Ba Trung, Ha Noi, VN");
        vars.put("orderUrl", "http://localhost:5173/account/orders/VST-RENDER-1");
        vars.put("supportEmail", "support@vesta.test");
        vars.put("year", 2026);
        Context ctx = new Context();
        ctx.setVariables(vars);
        return ctx;
    }

    @Test
    void allOrderEmailTemplatesRenderWithBrandingAndData() {
        for (String template : TEMPLATES) {
            String html = templateEngine.process(template, sampleContext());
            assertThat(html).as("template %s", template)
                    .contains("VESTA")
                    .contains("VST-RENDER-1")
                    .contains("510,000")
                    .contains("support@vesta.test");
        }
    }
}
