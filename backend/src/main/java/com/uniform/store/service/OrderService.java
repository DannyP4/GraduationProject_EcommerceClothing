package com.uniform.store.service;

import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderDetailDto placeOrder(String email, PlaceOrderRequest req);

    PageResponse<OrderSummaryDto> listOrders(String email, Pageable pageable);

    OrderDetailDto getOrder(String email, String orderNumber);

    OrderDetailDto cancelOrder(String email, String orderNumber);
}
