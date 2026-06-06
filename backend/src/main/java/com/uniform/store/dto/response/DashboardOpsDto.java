package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOpsDto {
    private final long openOrders;
    private final long lowStock;
    private final int lowStockThreshold;
}
