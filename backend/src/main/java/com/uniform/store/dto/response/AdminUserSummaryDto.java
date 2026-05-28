package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserSummaryDto {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String roleName;
    private final UserStatus status;
    private final Instant lastLoginAt;
    private final Instant createdAt;
    private final long ordersCount;
}
