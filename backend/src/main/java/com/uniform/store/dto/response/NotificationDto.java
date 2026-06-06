package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationDto {
    private final String id;
    private final String type;
    private final String message;
    private final String href;
    private final Instant at;
}
