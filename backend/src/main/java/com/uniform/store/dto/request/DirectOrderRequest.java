package com.uniform.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DirectOrderRequest {

    @NotNull(message = "variantId is required")
    private Long variantId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "addressId is required")
    private Long addressId;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    @Size(max = 500, message = "notes cannot exceed 500 characters")
    private String notes;

    @Size(max = 50, message = "couponCode cannot exceed 50 characters")
    private String couponCode;
}
