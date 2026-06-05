package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminCouponDto {

    private Long id;
    private String code;
    private CouponType type;
    private BigDecimal value;
    private CouponScope scope;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Instant startsAt;
    private Instant endsAt;
    private Integer maxUses;
    private Integer maxUsesPerUser;
    private Integer usedCount;
    private CouponStatus status;
    private Set<Long> categoryIds;
    private Set<Long> productIds;
    private Instant createdAt;
    private Instant updatedAt;
}
