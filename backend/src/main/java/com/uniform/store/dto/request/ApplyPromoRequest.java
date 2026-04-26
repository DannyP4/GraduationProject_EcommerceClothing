package com.uniform.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplyPromoRequest {

    @NotBlank
    private String code;
}
