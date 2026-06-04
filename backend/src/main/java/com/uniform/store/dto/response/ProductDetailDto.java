package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailDto {
    private Long id;
    private String slug;
    private String name;
    private String description;
    private Gender gender;
    private BigDecimal basePrice;
    private String currency;
    private String brandName;
    private String brandSlug;
    private String categoryName;
    private String categorySlug;
    private List<ProductImageDto> images;
    private List<ProductVariantDto> variants;
    private Map<String, String> attributes;
    private Double averageRating;
    private Long reviewCount;
    private Long soldCount;
}
