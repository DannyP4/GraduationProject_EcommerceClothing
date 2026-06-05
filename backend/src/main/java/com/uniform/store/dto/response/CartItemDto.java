package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDto {

    private Long id;
    private Long variantId;
    private String sku;

    private Long productId;
    private String productSlug;
    private String productName;

    private String size;
    private String color;
    private String colorHex;
    private String imageUrl;

    private BigDecimal unitPrice;
    private BigDecimal originalUnitPrice;
    private String currency;
    private Integer quantity;
    private BigDecimal lineTotal;

    private StockStatus stockStatus;
    private Integer stockQuantity;
}
