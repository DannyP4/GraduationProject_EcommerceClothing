package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AdminInviteResponse {

    private String email;
    private Instant expiresAt;
}
