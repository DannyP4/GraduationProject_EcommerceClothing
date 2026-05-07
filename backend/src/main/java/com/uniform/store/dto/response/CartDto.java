package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartDto {

    // null until the cart row is lazily created on first POST /cart/items.
    private Long id;
    private List<CartItemDto> items;
    private Integer itemCount;
    private BigDecimal subtotal;
    private String currency;
}
