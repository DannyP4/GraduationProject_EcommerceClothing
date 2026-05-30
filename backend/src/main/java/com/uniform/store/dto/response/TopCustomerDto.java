package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopCustomerDto {
    private final Long userId;
    private final String email;
    private final String fullName;
    private final long orderCount;
    private final BigDecimal totalSpent;
}
