package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    private Long parentId;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase kebab-case")
    @Size(max = 100)
    private String slug;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 150)
    private String nameEn;

    @PositiveOrZero
    private Integer sortOrder = 0;

    private Boolean isActive = true;
}
