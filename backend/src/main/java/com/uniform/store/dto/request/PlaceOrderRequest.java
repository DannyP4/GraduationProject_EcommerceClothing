package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "addressId is required")
    private Long addressId;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    @Size(max = 500, message = "notes cannot exceed 500 characters")
    private String notes;

    @Size(max = 50, message = "couponCode cannot exceed 50 characters")
    private String couponCode;
}
