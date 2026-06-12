package com.uniform.store.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBrandRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String websiteUrl;

    private String descriptionVi;

    private String descriptionEn;

    private String descriptionJa;

    private Boolean isActive;
}
