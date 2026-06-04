package com.uniform.store.service;

import com.uniform.store.dto.response.AdminOrderDetailDto;

public interface RefundService {

    AdminOrderDetailDto refundOrder(String orderNumber, String reason, String actorEmail);
}
