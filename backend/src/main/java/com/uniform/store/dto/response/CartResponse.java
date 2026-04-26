package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long id;
    private List<CartItemInfo> items;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String appliedCouponCode;
    private String appliedCouponDescription;
    private boolean freeShipping;

    @Data
    @Builder
    public static class CartItemInfo {
        private Long id;
        private Long variantId;
        private String productName;
        private String productSlug;
        private String size;
        private String color;
        private String sku;
        private String imageUrl;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal;
        private int stockQty;
    }
}
