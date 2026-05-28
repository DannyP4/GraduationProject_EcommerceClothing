package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProductImageDto {

    private final Long id;
    private final Long productId;
    private final Long variantId;
    private final String url;
    private final String publicId;
    private final String altText;
    private final Integer sortOrder;
    private final Boolean isPrimary;
    private final Instant createdAt;
    private final Instant updatedAt;
}
