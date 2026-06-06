package com.uniform.store.dto.request;

import com.uniform.store.enums.ShippingRegion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAddressRequest {

    @Size(max = 50, message = "label must be at most 50 characters")
    private String label;

    @NotBlank(message = "recipient is required")
    @Size(max = 150)
    private String recipient;

    @NotBlank(message = "phone is required")
    @Pattern(
            regexp = "^[0-9+\\-\\s()]{6,20}$",
            message = "phone must be 6–20 chars containing digits, +, -, spaces or parentheses")
    private String phone;

    @NotBlank(message = "line1 is required")
    @Size(max = 255)
    private String line1;

    @Size(max = 100)
    private String ward;

    @NotBlank(message = "district is required")
    @Size(max = 100)
    private String district;

    @NotBlank(message = "city is required")
    @Size(max = 100)
    private String city;

    // Defaults to VN in service if absent.
    @Pattern(regexp = "^[A-Z]{2}$", message = "country must be a 2-letter ISO code (e.g. VN)")
    private String country;

    @Size(max = 20)
    private String postalCode;

    // Region tier for shipping (NORTH/CENTRAL/SOUTH).
    private ShippingRegion region;

    // If true (or this is the user's first address), set as default and un-flag others.
    private Boolean isDefault;
}
