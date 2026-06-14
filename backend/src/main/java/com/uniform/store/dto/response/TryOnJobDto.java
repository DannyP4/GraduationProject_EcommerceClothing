package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TryOnJobDto {

    private final Long id;
    private final Long productId;
    private final String status;
    private final String userImageUrl;
    private final String garmentImageUrl;
    private final String resultImageUrl;
    private final String errorMessage;
    private final boolean cached;
    private final Instant createdAt;
}
