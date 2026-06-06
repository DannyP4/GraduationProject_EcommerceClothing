package com.uniform.store.dto.request;

import com.uniform.store.enums.ShippingRegion;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAddressRequest { 
    
    @Size(max = 50)
    private String label;

    @Size(min = 1, max = 150)
    private String recipient;

    @Pattern(
            regexp = "^[0-9+\\-\\s()]{6,20}$",
            message = "phone must be 6–20 chars containing digits, +, -, spaces or parentheses")
    private String phone;

    @Size(min = 1, max = 255)
    private String line1;

    @Size(max = 100)
    private String ward;

    @Size(min = 1, max = 100)
    private String district;

    @Size(min = 1, max = 100)
    private String city;

    @Pattern(regexp = "^[A-Z]{2}$", message = "country must be a 2-letter ISO code (e.g. VN)")
    private String country;

    @Size(max = 20)
    private String postalCode;

    private ShippingRegion region;
}
