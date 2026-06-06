package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ShippingQuoteDto {
    private final BigDecimal fee;
    private final BigDecimal freeThreshold;
}
