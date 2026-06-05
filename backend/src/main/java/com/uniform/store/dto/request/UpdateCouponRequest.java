package com.uniform.store.dto.request;

import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
public class UpdateCouponRequest {

    @NotNull
    private CouponType type;

    @NotNull
    @DecimalMin(value = "0.0", message = "value must be non-negative")
    private BigDecimal value;

    private CouponScope scope;

    @DecimalMin(value = "0.0", message = "minOrderAmount must be non-negative")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "maxDiscountAmount must be non-negative")
    private BigDecimal maxDiscountAmount;

    private Instant startsAt;

    private Instant endsAt;

    @Min(value = 1, message = "maxUses must be at least 1")
    private Integer maxUses;

    @Min(value = 1, message = "maxUsesPerUser must be at least 1")
    private Integer maxUsesPerUser;

    @NotNull
    private CouponStatus status;

    private Set<Long> categoryIds;

    private Set<Long> productIds;
}
