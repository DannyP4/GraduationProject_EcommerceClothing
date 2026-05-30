package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatsSummaryDto {

    private final LocalDate from;
    private final LocalDate to;
    private final Period current;
    private final Period previous;
    private final Changes changes;

    @Getter
    @Builder
    public static class Period {
        private final BigDecimal revenue;
        private final long orders;
        private final BigDecimal avgOrderValue;
        private final long newCustomers;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Changes {
        private final Double revenuePct;
        private final Double ordersPct;
        private final Double avgOrderValuePct;
        private final Double newCustomersPct;
    }
}
