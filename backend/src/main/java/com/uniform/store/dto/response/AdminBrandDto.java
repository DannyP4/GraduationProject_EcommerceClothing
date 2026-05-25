package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminBrandDto {

    private final Long id;
    private final String slug;
    private final String name;
    private final String logoUrl;
    private final String websiteUrl;
    private final Boolean isActive;
    private final String descriptionVi;
    private final String descriptionEn;
    private final Long productCount;
    private final Instant createdAt;
    private final Instant updatedAt;
}
