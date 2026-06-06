package com.uniform.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CouponValidateRequest {

    @NotBlank(message = "code is required")
    @Size(max = 50)
    private String code;

    private Long variantId;

    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;
}
