package com.uniform.store.service.impl;

import com.uniform.store.config.AppMailProperties;
import com.uniform.store.dto.mail.OrderEmailModel;
import com.uniform.store.enums.EmailStatus;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.service.EmailLogService;
import com.uniform.store.service.MailService;
import com.uniform.store.service.OrderMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMailServiceImpl implements OrderMailService {

    private final AppMailProperties mailProps;
    private final OrderEmailModelFactory modelFactory;
    private final MailService mailService;
    private final EmailLogService emailLogService;

    @Override
    public void sendOrderEmail(Long orderId, OrderEmailType type) {
        if (!mailProps.isEnabled()) {
            log.debug("Mail disabled — skip {} for order {}", type, orderId);
            return;
        }
        if (emailLogService.alreadySent(orderId, type)) {
            log.debug("Skip already-sent {} for order {}", type, orderId);
            return;
        }

        OrderEmailModel model;
        try {
            model = modelFactory.build(orderId);
        } catch (Exception e) {
            log.warn("Failed to build email model for order {} ({})", orderId, type, e);
            return;
        }

        String orderNumber = String.valueOf(model.vars().get("orderNumber"));
        String subject = subjectFor(type, orderNumber);
        String template = templateFor(type);

        try {
            mailService.send(model.recipient(), subject, template, model.vars());
            emailLogService.record(orderId, type, model.recipient(), subject, EmailStatus.SENT, null);
            log.info("Sent {} email for order {}", type, orderId);
        } catch (Exception e) {
            emailLogService.record(orderId, type, model.recipient(), subject, EmailStatus.FAILED, e.getMessage());
            log.warn("Failed to send {} email for order {}", type, orderId, e);
        }
    }

    private String subjectFor(OrderEmailType type, String orderNumber) {
        return switch (type) {
            case CONFIRMATION -> "Vesta — Order #" + orderNumber + " received";
            case PAYMENT_RECEIVED -> "Vesta — Payment received for #" + orderNumber;
            case SHIPPED -> "Vesta — Your order #" + orderNumber + " has shipped";
            case DELIVERED -> "Vesta — Order #" + orderNumber + " delivered";
            case CANCELLED -> "Vesta — Order #" + orderNumber + " cancelled";
            case REFUNDED -> "Vesta — Refund issued for #" + orderNumber;
        };
    }

    private String templateFor(OrderEmailType type) {
        return switch (type) {
            case CONFIRMATION -> "email/order-confirmation";
            case PAYMENT_RECEIVED -> "email/payment-received";
            case SHIPPED -> "email/order-shipped";
            case DELIVERED -> "email/order-delivered";
            case CANCELLED -> "email/order-cancelled";
            case REFUNDED -> "email/order-refunded";
        };
    }
}
