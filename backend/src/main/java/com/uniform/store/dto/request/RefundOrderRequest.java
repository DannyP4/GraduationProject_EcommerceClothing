package com.uniform.store.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefundOrderRequest {

    @Size(max = 400)
    private String reason;
}
