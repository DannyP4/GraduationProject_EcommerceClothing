package com.uniform.store.service;

import com.uniform.store.enums.EmailStatus;
import com.uniform.store.enums.OrderEmailType;

public interface EmailLogService {

    boolean alreadySent(Long orderId, OrderEmailType type);

    void record(Long orderId, OrderEmailType type, String recipient,
                String subject, EmailStatus status, String error);

    void record(Long orderId, String type, String recipient,
                String subject, EmailStatus status, String error);
}
