package com.uniform.store.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    private Long parentId;

    private Boolean clearParent;

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 150)
    private String nameVi;

    @Size(max = 150)
    private String nameJa;

    @PositiveOrZero
    private Integer sortOrder;

    private Boolean isActive;
}
