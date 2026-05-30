package com.uniform.store.enums;

import java.util.EnumSet;
import java.util.Set;

public final class RevenueStatuses {

    public static final Set<OrderStatus> REVENUE = EnumSet.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private RevenueStatuses() {}
}
