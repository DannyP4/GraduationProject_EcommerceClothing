package com.uniform.store.service.impl;

import com.uniform.store.config.AppNotificationProperties;
import com.uniform.store.dto.response.NotificationResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Notification;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.User;
import com.uniform.store.enums.NotificationType;
import com.uniform.store.enums.OrderEmailType;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.NotificationRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AppNotificationProperties notificationProperties;

    @Override
    @Transactional
    public void createForOrderEvent(Long orderId, OrderEmailType type) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Skip notification - order {} not found", orderId);
            return;
        }
        NotificationType notificationType = mapType(type);
        notificationRepository.save(Notification.builder()
                .userId(order.getUser().getId())
                .type(notificationType)
                .message(messageFor(notificationType, order.getOrderNumber()))
                .href("/account/orders/" + order.getOrderNumber())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(String email, Pageable pageable) {
        User user = loadUser(email);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Pageable safe = PageRequest.of(Math.max(pageable.getPageNumber(), 0), safeSize);

        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), safe);
        List<NotificationResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByUserIdAndReadAtIsNull(loadUser(email).getId());
    }

    @Override
    @Transactional
    public void markRead(String email, Long id) {
        User user = loadUser(email);
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int markAllRead(String email) {
        return notificationRepository.markAllRead(loadUser(email).getId(), Instant.now());
    }

    @Override
    @Transactional
    public int purgeExpired() {
        int days = Math.max(notificationProperties.getRetentionDays(), 1);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Purged {} notifications older than {} days", deleted, days);
        }
        return deleted;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .message(n.getMessage())
                .href(n.getHref())
                .read(n.getReadAt() != null)
                .createdAt(n.getCreatedAt())
                .build();
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private NotificationType mapType(OrderEmailType type) {
        return switch (type) {
            case CONFIRMATION -> NotificationType.ORDER_PLACED;
            case PAYMENT_RECEIVED -> NotificationType.ORDER_PAID;
            case SHIPPED -> NotificationType.ORDER_SHIPPED;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            case REFUNDED -> NotificationType.ORDER_REFUNDED;
        };
    }

    private String messageFor(NotificationType type, String orderNumber) {
        return switch (type) {
            case ORDER_PLACED -> "Your order #" + orderNumber + " has been placed.";
            case ORDER_PAID -> "We've received payment for order #" + orderNumber + ".";
            case ORDER_SHIPPED -> "Order #" + orderNumber + " is on its way.";
            case ORDER_DELIVERED -> "Order #" + orderNumber + " has been delivered.";
            case ORDER_CANCELLED -> "Order #" + orderNumber + " has been cancelled.";
            case ORDER_REFUNDED -> "A refund for order #" + orderNumber + " has been issued.";
        };
    }
}
