package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminCategoryDto {

    private final Long id;
    private final Long parentId;
    private final String slug;
    private final String name;
    private final String imageUrl;
    private final String nameVi;
    private final String nameJa;
    private final Integer sortOrder;
    private final Boolean isActive;
    private final Long productCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<AdminCategoryDto> children;
}
