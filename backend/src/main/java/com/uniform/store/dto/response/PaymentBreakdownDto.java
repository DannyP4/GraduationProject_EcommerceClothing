package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.PaymentProvider;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentBreakdownDto {
    private final PaymentProvider provider;
    private final BigDecimal revenue;
    private final long orderCount;
    private final double pct;
}
