package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBrandRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase kebab-case")
    @Size(max = 100)
    private String slug;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String websiteUrl;

    private String descriptionVi;

    private String descriptionEn;

    private String descriptionJa;

    private Boolean isActive = true;
}
