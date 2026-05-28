package com.uniform.store.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductImageRequest {

    @Size(max = 255)
    private String altText;

    @PositiveOrZero
    private Integer sortOrder;

    private Boolean isPrimary;

    private Long variantId;

    private Boolean clearVariant;
}
