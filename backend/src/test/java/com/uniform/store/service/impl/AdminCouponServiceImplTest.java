package com.uniform.store.service.impl;

import com.uniform.store.dto.request.CreateCouponRequest;
import com.uniform.store.dto.response.AdminCouponDto;
import com.uniform.store.entity.Coupon;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderCouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceImplTest {

    @Mock CouponRepository couponRepository;
    @Mock OrderCouponRepository orderCouponRepository;

    @InjectMocks AdminCouponServiceImpl service;

    private CreateCouponRequest req(CouponType type, String value, CouponScope scope) {
        CreateCouponRequest r = new CreateCouponRequest();
        r.setCode("welcome");
        r.setType(type);
        r.setValue(new BigDecimal(value));
        r.setScope(scope);
        return r;
    }

    @Test
    void create_duplicateCode_throws() {
        CreateCouponRequest r = req(CouponType.PERCENT, "10", CouponScope.ALL);
        when(couponRepository.existsByCode("WELCOME")).thenReturn(true);

        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_percentOver100_throws() {
        CreateCouponRequest r = req(CouponType.PERCENT, "150", CouponScope.ALL);
        when(couponRepository.existsByCode("WELCOME")).thenReturn(false);

        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed 100");
    }

    @Test
    void create_categoryScopeWithoutIds_throws() {
        CreateCouponRequest r = req(CouponType.PERCENT, "10", CouponScope.CATEGORY);
        when(couponRepository.existsByCode("WELCOME")).thenReturn(false);

        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CATEGORY scope requires");
    }

    @Test
    void create_endsBeforeStarts_throws() {
        CreateCouponRequest r = req(CouponType.PERCENT, "10", CouponScope.ALL);
        Instant start = Instant.now();
        r.setStartsAt(start);
        r.setEndsAt(start.minusSeconds(3600));
        when(couponRepository.existsByCode("WELCOME")).thenReturn(false);

        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("endsAt must be after");
    }

    @Test
    void create_valid_savesUppercaseCodeAndScopeIds() {
        CreateCouponRequest r = req(CouponType.PERCENT, "10", CouponScope.CATEGORY);
        r.setCategoryIds(Set.of(2L, 3L));
        when(couponRepository.existsByCode("WELCOME")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> {
            Coupon c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        AdminCouponDto dto = service.create(r);

        assertThat(dto.getCode()).isEqualTo("WELCOME");
        assertThat(dto.getScope()).isEqualTo(CouponScope.CATEGORY);
        assertThat(dto.getCategoryIds()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(dto.getProductIds()).isEmpty();
    }

    @Test
    void delete_usedCoupon_blocked() {
        Coupon c = Coupon.builder().code("SAVE").type(CouponType.PERCENT).value(new BigDecimal("10")).build();
        c.setId(5L);
        when(couponRepository.findById(5L)).thenReturn(java.util.Optional.of(c));
        when(orderCouponRepository.existsByCouponId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Disable it instead");
        verify(couponRepository, never()).delete(any());
    }

    @Test
    void delete_unusedCoupon_deletes() {
        Coupon c = Coupon.builder().code("SAVE").type(CouponType.PERCENT).value(new BigDecimal("10")).build();
        c.setId(5L);
        when(couponRepository.findById(5L)).thenReturn(java.util.Optional.of(c));
        when(orderCouponRepository.existsByCouponId(5L)).thenReturn(false);

        service.delete(5L);

        verify(couponRepository).delete(c);
    }
}
