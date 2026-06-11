package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthExchangeRequest {

    @NotBlank
    private String code;
}
