package com.uniform.store.dto.request;

import com.uniform.store.enums.Gender;
import com.uniform.store.enums.SaleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase kebab-case")
    @Size(max = 150)
    private String slug;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    @NotNull
    private Long brandId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Gender gender;

    @NotNull
    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice;

    private SaleType saleType;

    @DecimalMin(value = "0.0", message = "Sale value must be non-negative")
    private BigDecimal saleValue;

    private Instant saleStartsAt;

    private Instant saleEndsAt;

    private Boolean isActive = true;

    private Instant publishedAt;
}
