package com.uniform.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one digit")
    private String password;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Pattern(regexp = "^(en|vi|ja)$", message = "preferredLocale must be one of: en, vi, ja")
    private String preferredLocale;

    private String captchaToken;
}
