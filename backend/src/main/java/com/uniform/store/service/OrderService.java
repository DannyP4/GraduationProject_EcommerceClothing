package com.uniform.store.service;

import com.uniform.store.dto.request.DirectOrderRequest;
import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.PlaceOrderResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    PlaceOrderResponse placeOrder(String email, PlaceOrderRequest req, String clientIp);

    PlaceOrderResponse placeDirectOrder(String email, DirectOrderRequest req, String clientIp);

    PageResponse<OrderSummaryDto> listOrders(String email, Pageable pageable, String locale);

    OrderDetailDto getOrder(String email, String orderNumber, String locale);

    OrderDetailDto cancelOrder(String email, String orderNumber);

    List<Long> findStalePendingOrderIds();

    boolean autoCancelStaleOrder(Long orderId);
}
