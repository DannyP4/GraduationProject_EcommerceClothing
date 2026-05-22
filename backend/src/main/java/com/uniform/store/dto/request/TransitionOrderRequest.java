package com.uniform.store.dto.request;

import com.uniform.store.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransitionOrderRequest {

    @NotNull
    private OrderStatus targetStatus;

    @Size(max = 400)
    private String note;
}
