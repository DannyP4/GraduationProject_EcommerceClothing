package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {
    private final Long id;
    private final String type;
    private final String message;
    private final String href;
    private final boolean read;
    private final Instant createdAt;
}
