package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String slug;
    private String name;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private Integer discountPercent;
    private String currency;
    private String primaryImageUrl;
    private String brandName;
    private String categoryName;
    private Double averageRating;
    private Long reviewCount;
    private Long soldCount;
}
