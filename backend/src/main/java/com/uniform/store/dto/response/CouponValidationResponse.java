package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponValidationResponse {

    private String code;
    private CouponType type;
    private CouponScope scope;
    private BigDecimal value;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal totalAfterDiscount;
    private String message;
}
