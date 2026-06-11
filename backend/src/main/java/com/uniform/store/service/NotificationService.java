package com.uniform.store.service;

import com.uniform.store.dto.response.NotificationResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.enums.OrderEmailType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createForOrderEvent(Long orderId, OrderEmailType type);

    PageResponse<NotificationResponse> listForUser(String email, Pageable pageable);

    long unreadCount(String email);

    void markRead(String email, Long id);

    int markAllRead(String email);

    int purgeExpired();
}
