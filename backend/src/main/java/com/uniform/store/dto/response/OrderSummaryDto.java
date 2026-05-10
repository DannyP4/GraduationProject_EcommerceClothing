package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryDto {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private Integer itemCount;
    private BigDecimal grandTotal;
    private String currency;
    private Instant placedAt;

    // Cosmetic — first item name + count, plus first item's product thumbnail
    private String firstItemName;
    private String thumbnailUrl;
}
