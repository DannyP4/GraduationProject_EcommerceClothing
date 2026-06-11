package com.uniform.store.service;

import com.uniform.store.enums.OrderEmailType;

public interface OrderMailService {

    void sendOrderEmail(Long orderId, OrderEmailType type);
}
