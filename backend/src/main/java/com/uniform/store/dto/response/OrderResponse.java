package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String status;

    // Shipping info
    private String shippingName;
    private String shippingLine1;
    private String shippingLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostal;
    private String shippingCountry;

    // Financials
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCodeSnapshot;

    private String notes;
    private Instant createdAt;
    private List<OrderItemInfo> items;

    @Data
    @Builder
    public static class OrderItemInfo {
        private Long id;
        private String productName;
        private String productSlug;
        private String size;
        private String color;
        private String imageUrl;
        private BigDecimal unitPrice;
        private short quantity;
        private BigDecimal lineTotal;
    }
}
