package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProductSummaryDto {

    private final Long id;
    private final String slug;
    private final String name;
    private final Gender gender;
    private final BigDecimal basePrice;
    private final String currency;
    private final Boolean isActive;
    private final Instant deletedAt;
    private final Instant publishedAt;
    private final BrandRef brand;
    private final CategoryRef category;
    private final String primaryImageUrl;
    private final Long variantCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Getter
    @Builder
    public static class BrandRef {
        private final Long id;
        private final String name;
    }

    @Getter
    @Builder
    public static class CategoryRef {
        private final Long id;
        private final String name;
    }
}
