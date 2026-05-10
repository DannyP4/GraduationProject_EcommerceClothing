package com.uniform.store.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 150, message = "fullName must be 1–150 characters")
    private String fullName;

    @Pattern(
            regexp = "^[0-9+\\-\\s()]{6,20}$",
            message = "phone must be 6–20 chars containing digits, +, -, spaces or parentheses")
    private String phone;

    @Pattern(regexp = "^(en|vi|ja)$", message = "preferredLocale must be one of: en, vi, ja")
    private String preferredLocale;
}
