package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.Gender;
import com.uniform.store.enums.SaleType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProductDetailDto {

    private final Long id;
    private final String slug;
    private final String name;
    private final String description;
    private final Gender gender;
    private final BigDecimal basePrice;
    private final SaleType saleType;
    private final BigDecimal saleValue;
    private final Instant saleStartsAt;
    private final Instant saleEndsAt;
    private final String currency;
    private final Boolean isActive;
    private final Instant publishedAt;
    private final Instant deletedAt;
    private final AdminProductSummaryDto.BrandRef brand;
    private final AdminProductSummaryDto.CategoryRef category;
    private final List<AdminVariantDto> variants;
    private final List<AdminProductImageDto> images;
    private final Instant createdAt;
    private final Instant updatedAt;
}
