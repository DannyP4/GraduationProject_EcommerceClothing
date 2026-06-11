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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;

    NotificationServiceImpl service;
    AppNotificationProperties props;

    @BeforeEach
    void setup() {
        props = new AppNotificationProperties();
        props.setRetentionDays(90);
        service = new NotificationServiceImpl(notificationRepository, orderRepository, userRepository, props);
    }

    private User user(Long id, String email) {
        User u = User.builder().email(email).build();
        u.setId(id);
        return u;
    }

    @Test
    void createForOrderEvent_mapsTypeAndPersistsForOrderOwner() {
        User buyer = user(7L, "buyer@uniform.test");
        Order order = Order.builder().orderNumber("ORD-1").user(buyer).build();
        order.setId(42L);
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));

        service.createForOrderEvent(42L, OrderEmailType.SHIPPED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_SHIPPED);
        assertThat(saved.getMessage()).contains("ORD-1");
        assertThat(saved.getHref()).isEqualTo("/account/orders/ORD-1");
        assertThat(saved.getReadAt()).isNull();
    }

    @Test
    void createForOrderEvent_skipsWhenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        service.createForOrderEvent(99L, OrderEmailType.CONFIRMATION);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_setsReadAtForOwnedUnreadNotification() {
        User buyer = user(7L, "buyer@uniform.test");
        Notification n = Notification.builder().userId(7L).type(NotificationType.ORDER_PLACED).build();
        n.setId(5L);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(notificationRepository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(n));

        service.markRead("buyer@uniform.test", 5L);

        assertThat(n.getReadAt()).isNotNull();
        verify(notificationRepository).save(n);
    }

    @Test
    void markRead_isIdempotentForAlreadyReadNotification() {
        User buyer = user(7L, "buyer@uniform.test");
        Notification n = Notification.builder().userId(7L).type(NotificationType.ORDER_PLACED)
                .readAt(Instant.parse("2026-06-12T00:00:00Z")).build();
        n.setId(5L);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(notificationRepository.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(n));

        service.markRead("buyer@uniform.test", 5L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_throwsWhenNotificationNotOwnedByUser() {
        User other = user(8L, "other@uniform.test");
        when(userRepository.findByEmail("other@uniform.test")).thenReturn(Optional.of(other));
        when(notificationRepository.findByIdAndUserId(5L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead("other@uniform.test", 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unreadCount_delegatesToRepositoryWithUserId() {
        User buyer = user(7L, "buyer@uniform.test");
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(notificationRepository.countByUserIdAndReadAtIsNull(7L)).thenReturn(3L);

        assertThat(service.unreadCount("buyer@uniform.test")).isEqualTo(3L);
    }

    @Test
    void markAllRead_returnsAffectedCount() {
        User buyer = user(7L, "buyer@uniform.test");
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(notificationRepository.markAllRead(eq(7L), any(Instant.class))).thenReturn(4);

        assertThat(service.markAllRead("buyer@uniform.test")).isEqualTo(4);
    }

    @Test
    void listForUser_mapsReadFlagFromReadAt() {
        User buyer = user(7L, "buyer@uniform.test");
        Notification unread = Notification.builder().userId(7L).type(NotificationType.ORDER_PLACED)
                .message("a").href("/h").build();
        unread.setId(1L);
        Notification read = Notification.builder().userId(7L).type(NotificationType.ORDER_SHIPPED)
                .message("b").href("/h").readAt(Instant.parse("2026-06-12T00:00:00Z")).build();
        read.setId(2L);
        when(userRepository.findByEmail("buyer@uniform.test")).thenReturn(Optional.of(buyer));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(unread, read), PageRequest.of(0, 10), 2));

        PageResponse<NotificationResponse> page = service.listForUser("buyer@uniform.test", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(NotificationResponse::isRead)
                .containsExactly(false, true);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void purgeExpired_deletesOlderThanRetentionWindow() {
        props.setRetentionDays(90);
        when(notificationRepository.deleteOlderThan(any(Instant.class))).thenReturn(7);

        int deleted = service.purgeExpired();

        assertThat(deleted).isEqualTo(7);
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(notificationRepository).deleteOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(Instant.now().minus(89, ChronoUnit.DAYS));
        assertThat(cutoff.getValue()).isAfter(Instant.now().minus(91, ChronoUnit.DAYS));
    }
}
