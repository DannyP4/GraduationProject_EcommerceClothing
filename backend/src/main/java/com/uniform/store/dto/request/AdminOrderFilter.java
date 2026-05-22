package com.uniform.store.dto.request;

import com.uniform.store.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderFilter {

    private OrderStatus status;
    private Instant placedFrom;
    private Instant placedTo;
    private String search;
}
