package com.uniform.store.service;

import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
    OrderResponse placeOrder(Long userId, PlaceOrderRequest req);
    Page<OrderResponse> getUserOrders(Long userId, int page);
    OrderResponse getOrderById(Long userId, Long orderId);
}
