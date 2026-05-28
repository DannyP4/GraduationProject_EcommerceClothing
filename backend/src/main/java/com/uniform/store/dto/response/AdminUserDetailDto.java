package com.uniform.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDetailDto {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String preferredLocale;
    private final String roleName;
    private final UserStatus status;
    private final Instant emailVerifiedAt;
    private final Instant lastLoginAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<AddressRef> addresses;
    private final List<OrderRef> orders;
    private final long ordersCount;

    @Getter
    @Builder
    public static class AddressRef {
        private final Long id;
        private final String label;
        private final String recipient;
        private final String phone;
        private final String line1;
        private final String ward;
        private final String district;
        private final String city;
        private final String country;
        private final String postalCode;
        private final boolean isDefault;
    }

    @Getter
    @Builder
    public static class OrderRef {
        private final String orderNumber;
        private final OrderStatus status;
        private final BigDecimal grandTotal;
        private final String currency;
        private final Instant placedAt;
    }
}
