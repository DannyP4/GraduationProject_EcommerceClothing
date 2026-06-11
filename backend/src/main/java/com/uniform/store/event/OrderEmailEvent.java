package com.uniform.store.event;

import com.uniform.store.enums.OrderEmailType;

public record OrderEmailEvent(Long orderId, OrderEmailType type) {}
