package com.uniform.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {

    // quantity == 0 is a valid request — service treats it as a delete (FE convenience).
    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be >= 0")
    private Integer quantity;
}
