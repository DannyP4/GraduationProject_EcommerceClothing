package com.uniform.store.service.impl;

import com.uniform.store.entity.EmailLog;
import com.uniform.store.enums.EmailStatus;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.repository.EmailLogRepository;
import com.uniform.store.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailLogServiceImpl implements EmailLogService {

    private static final int MAX_ERROR_LEN = 1000;

    private final EmailLogRepository emailLogRepository;

    @Override
    public boolean alreadySent(Long orderId, OrderEmailType type) {
        return orderId != null
                && emailLogRepository.existsByOrderIdAndTypeAndStatus(orderId, type.name(), EmailStatus.SENT);
    }

    @Override
    public void record(Long orderId, OrderEmailType type, String recipient,
                       String subject, EmailStatus status, String error) {
        record(orderId, type.name(), recipient, subject, status, error);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long orderId, String type, String recipient,
                       String subject, EmailStatus status, String error) {
        emailLogRepository.save(EmailLog.builder()
                .orderId(orderId)
                .type(type)
                .recipient(recipient)
                .subject(subject)
                .status(status)
                .error(truncate(error))
                .build());
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_ERROR_LEN ? value : value.substring(0, MAX_ERROR_LEN);
    }
}
