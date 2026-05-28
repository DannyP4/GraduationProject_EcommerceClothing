package com.uniform.store.service.impl;

import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks private AdminUserServiceImpl service;

    private Role customerRole;
    private Role adminRole;
    private User customer;
    private User admin;

    @BeforeEach
    void setup() {
        customerRole = Role.builder().name(Role.CUSTOMER).displayName("Customer").build();
        customerRole.setId(1L);
        adminRole = Role.builder().name(Role.ADMIN).displayName("Administrator").build();
        adminRole.setId(2L);

        customer = User.builder()
                .email("jane@uniform.test").passwordHash("x").fullName("Jane")
                .preferredLocale("vi").role(customerRole).status(UserStatus.ACTIVE).build();
        customer.setId(10L);

        admin = User.builder()
                .email("admin@uniform.test").passwordHash("x").fullName("Admin")
                .preferredLocale("vi").role(adminRole).status(UserStatus.ACTIVE).build();
        admin.setId(20L);
    }

    @Test
    void get_missing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_returnsDetailWithOrdersAndAddresses() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(10L)).thenReturn(List.of());
        when(orderRepository.findTop50ByUserIdOrderByPlacedAtDesc(10L)).thenReturn(List.of());
        when(orderRepository.countByUserId(10L)).thenReturn(3L);

        var detail = service.get(10L);

        assertThat(detail.getEmail()).isEqualTo("jane@uniform.test");
        assertThat(detail.getRoleName()).isEqualTo("customer");
        assertThat(detail.getOrdersCount()).isEqualTo(3L);
    }

    @Test
    void suspend_admin_throws() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.suspend(20L, "other@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void suspend_self_throws() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.suspend(10L, "jane@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own");
    }

    @Test
    void suspend_alreadySuspended_throws() {
        customer.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.suspend(10L, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already suspended");
    }

    @Test
    void suspend_deleted_throws() {
        customer.setStatus(UserStatus.DELETED);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.suspend(10L, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void suspend_active_succeeds() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(10L)).thenReturn(List.of());
        when(orderRepository.findTop50ByUserIdOrderByPlacedAtDesc(10L)).thenReturn(List.of());
        when(orderRepository.countByUserId(10L)).thenReturn(0L);

        var detail = service.suspend(10L, "admin@uniform.test");

        assertThat(customer.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(detail.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void activate_alreadyActive_throws() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.activate(10L, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void activate_suspended_succeeds() {
        customer.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(10L)).thenReturn(List.of());
        when(orderRepository.findTop50ByUserIdOrderByPlacedAtDesc(10L)).thenReturn(List.of());
        when(orderRepository.countByUserId(10L)).thenReturn(0L);

        var detail = service.activate(10L, "admin@uniform.test");

        assertThat(customer.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(detail.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void softDelete_admin_throws() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.softDelete(20L, "other@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void softDelete_self_throws() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.softDelete(10L, "jane@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own");
    }

    @Test
    void softDelete_alreadyDeleted_throws() {
        customer.setStatus(UserStatus.DELETED);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.softDelete(10L, "admin@uniform.test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already deleted");
    }

    @Test
    void softDelete_active_succeeds() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(10L)).thenReturn(List.of());
        when(orderRepository.findTop50ByUserIdOrderByPlacedAtDesc(10L)).thenReturn(List.of());
        when(orderRepository.countByUserId(10L)).thenReturn(2L);

        var detail = service.softDelete(10L, "admin@uniform.test");

        assertThat(customer.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(detail.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(detail.getOrdersCount()).isEqualTo(2L);
    }
}
