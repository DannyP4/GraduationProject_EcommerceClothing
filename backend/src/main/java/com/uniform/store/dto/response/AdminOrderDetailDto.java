package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminOrderDetailDto {

    private OrderDetailDto order;
    private CustomerInfoDto customer;

    private Set<OrderStatus> allowedTransitions;
    private Boolean cancellableByAdmin;
    private Boolean refundableByAdmin;
    private Boolean requiresRefund;
}
