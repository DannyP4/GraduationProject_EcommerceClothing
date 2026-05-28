package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductImageRequest {

    @NotBlank
    @Size(max = 500)
    private String url;

    @Size(max = 255)
    private String publicId;

    @Size(max = 255)
    private String altText;

    @PositiveOrZero
    private Integer sortOrder;

    private Boolean isPrimary = false;

    private Long variantId;
}
