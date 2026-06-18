package com.uniform.store.service;

import com.uniform.store.dto.request.AdminOrderFilter;
import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.dto.response.AdminOrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    PageResponse<AdminOrderSummaryDto> listOrders(AdminOrderFilter filter, Pageable pageable);

    AdminOrderDetailDto getOrder(String orderNumber);

    AdminOrderDetailDto transitionOrder(String orderNumber, OrderStatus targetStatus, String note, String actorEmail);

    AdminOrderDetailDto cancelOrder(String orderNumber, String reason, String actorEmail);

    boolean markDeliveredFromGhn(Long orderId);
}
