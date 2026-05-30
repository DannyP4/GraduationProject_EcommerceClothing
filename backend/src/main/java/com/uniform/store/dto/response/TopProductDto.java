package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopProductDto {
    private final Long productId;
    private final String productName;
    private final long unitsSold;
    private final BigDecimal revenue;
}
