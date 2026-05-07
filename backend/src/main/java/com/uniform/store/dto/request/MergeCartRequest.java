package com.uniform.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest {

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<MergeItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeItem {
        @NotNull(message = "variantId is required")
        private Long variantId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
    }
}
