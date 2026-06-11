package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitePreviewResponse {

    private String email;
    private String fullName;
}
