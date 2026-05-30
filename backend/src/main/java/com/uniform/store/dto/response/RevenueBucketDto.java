package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueBucketDto {
    private final String bucket;
    private final BigDecimal revenue;
    private final long orderCount;
}
