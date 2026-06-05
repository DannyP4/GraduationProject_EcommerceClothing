package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ProductVariantDto {
    private Long id;
    private String sku;
    private String size;
    private String color;
    private String colorHex;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer discountPercent;
    private Integer stockQuantity;
    private Boolean isActive;
}
