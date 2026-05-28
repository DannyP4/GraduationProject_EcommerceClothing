package com.uniform.store.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateVariantRequest {

    @NotBlank
    @Size(max = 64)
    private String sku;

    @NotBlank
    @Size(max = 20)
    private String size;

    @NotBlank
    @Size(max = 50)
    private String color;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color hex must be #RRGGBB")
    private String colorHex;

    @DecimalMin(value = "0.0", message = "Price override must be non-negative")
    private BigDecimal priceOverride;

    @PositiveOrZero
    private Integer stockQuantity = 0;

    @PositiveOrZero
    private Integer weightGrams;

    private Boolean isActive = true;
}
