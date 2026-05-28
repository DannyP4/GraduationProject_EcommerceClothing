package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminVariantDto {

    private final Long id;
    private final Long productId;
    private final String sku;
    private final String size;
    private final String color;
    private final String colorHex;
    private final BigDecimal priceOverride;
    private final Integer stockQuantity;
    private final Integer weightGrams;
    private final Boolean isActive;
    private final Instant createdAt;
    private final Instant updatedAt;
}
