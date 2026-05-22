package com.uniform.store.enums;

import com.uniform.store.exception.BadRequestException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING,    EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.PAID,       EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED,    OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.SHIPPED,    EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.DELIVERED,  EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELLED,  EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REFUNDED,   EnumSet.noneOf(OrderStatus.class));
    }

    private OrderTransitions() {}

    public static Set<OrderStatus> allowedFrom(OrderStatus from) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return allowedFrom(from).contains(to);
    }

    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = allowedFrom(from);
        if (!allowed.contains(to)) {
            throw new BadRequestException(
                    "Invalid transition " + from + " -> " + to
                            + ". Allowed from " + from + ": "
                            + (allowed.isEmpty() ? "(terminal)" : allowed));
        }
    }

    public static boolean isCancellableByAdmin(OrderStatus current) {
        return current == OrderStatus.PENDING
                || current == OrderStatus.PAID
                || current == OrderStatus.PROCESSING;
    }
}
