package com.uniform.store.dto.request;

import com.uniform.store.enums.Gender;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private Long brandId;

    private Long categoryId;

    private Gender gender;

    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice;

    private Boolean isActive;

    private Instant publishedAt;
}
