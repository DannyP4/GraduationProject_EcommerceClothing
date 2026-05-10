package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemDto {

    private Long id;
    private Long variantId;

    // Snapshots — these are the source of truth for display, not the live product/variant.
    private String productName;
    private String variantLabel;
    private String sku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    // Live lookup — null if product/variant is gone (rare; we soft-delete) — for click-through and thumbnail.
    private String productSlug;
    private String imageUrl;
}
