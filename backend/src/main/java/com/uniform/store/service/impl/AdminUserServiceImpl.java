package com.uniform.store.service.impl;

import com.uniform.store.dto.request.AdminUserFilterRequest;
import com.uniform.store.dto.response.AdminUserDetailDto;
import com.uniform.store.dto.response.AdminUserSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.entity.Address;
import com.uniform.store.entity.Order;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.AddressRepository;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;

    @Override
    public PageResponse<AdminUserSummaryDto> list(AdminUserFilterRequest filter, Pageable pageable) {
        String search = (filter.getSearch() != null && !filter.getSearch().isBlank())
                ? filter.getSearch().trim()
                : null;

        Page<User> page = userRepository.searchAdmin(search, filter.getStatus(), pageable);

        List<Long> userIds = page.getContent().stream().map(User::getId).toList();
        Map<Long, Long> ordersCountByUser = new HashMap<>();
        for (Long uid : userIds) {
            ordersCountByUser.put(uid, orderRepository.countByUserId(uid));
        }

        List<AdminUserSummaryDto> mapped = page.getContent().stream()
                .map(u -> toSummary(u, ordersCountByUser.getOrDefault(u.getId(), 0L)))
                .toList();
        return PageResponse.from(page, mapped);
    }

    @Override
    public AdminUserDetailDto get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(id);
        List<Order> orders = orderRepository.findTop50ByUserIdOrderByPlacedAtDesc(id);
        long ordersCount = orderRepository.countByUserId(id);
        return toDetail(user, addresses, orders, ordersCount);
    }

    @Override
    @Transactional
    public AdminUserDetailDto suspend(Long id, String actingEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureNotAdmin(user, "suspend");
        ensureNotSelf(user, actingEmail, "suspend");
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BadRequestException("Cannot suspend a deleted user");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BadRequestException("User is already suspended");
        }
        user.setStatus(UserStatus.SUSPENDED);
        return get(id);
    }

    @Override
    @Transactional
    public AdminUserDetailDto activate(Long id, String actingEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("User is already active");
        }
        user.setStatus(UserStatus.ACTIVE);
        return get(id);
    }

    @Override
    @Transactional
    public AdminUserDetailDto softDelete(Long id, String actingEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureNotAdmin(user, "delete");
        ensureNotSelf(user, actingEmail, "delete");
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BadRequestException("User is already deleted");
        }
        user.setStatus(UserStatus.DELETED);
        return get(id);
    }

    private static void ensureNotAdmin(User user, String action) {
        if (Role.ADMIN.equalsIgnoreCase(user.getRole().getName())) {
            throw new BadRequestException("Cannot " + action + " an admin account");
        }
    }

    private static void ensureNotSelf(User user, String actingEmail, String action) {
        if (actingEmail != null && actingEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Cannot " + action + " your own account");
        }
    }

    private static AdminUserSummaryDto toSummary(User u, long ordersCount) {
        return AdminUserSummaryDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .roleName(u.getRole().getName())
                .status(u.getStatus())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .ordersCount(ordersCount)
                .build();
    }

    private static AdminUserDetailDto toDetail(User u, List<Address> addresses, List<Order> orders, long ordersCount) {
        return AdminUserDetailDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .preferredLocale(u.getPreferredLocale())
                .roleName(u.getRole().getName())
                .status(u.getStatus())
                .emailVerifiedAt(u.getEmailVerifiedAt())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .addresses(addresses.stream().map(AdminUserServiceImpl::addressToRef).toList())
                .orders(orders.stream().map(AdminUserServiceImpl::orderToRef).toList())
                .ordersCount(ordersCount)
                .build();
    }

    private static AdminUserDetailDto.AddressRef addressToRef(Address a) {
        return AdminUserDetailDto.AddressRef.builder()
                .id(a.getId())
                .label(a.getLabel())
                .recipient(a.getRecipient())
                .phone(a.getPhone())
                .line1(a.getLine1())
                .ward(a.getWard())
                .district(a.getDistrict())
                .city(a.getCity())
                .country(a.getCountry())
                .postalCode(a.getPostalCode())
                .isDefault(a.isDefault())
                .build();
    }

    private static AdminUserDetailDto.OrderRef orderToRef(Order o) {
        return AdminUserDetailDto.OrderRef.builder()
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .grandTotal(o.getGrandTotal())
                .currency(o.getCurrency())
                .placedAt(o.getPlacedAt())
                .build();
    }
}
