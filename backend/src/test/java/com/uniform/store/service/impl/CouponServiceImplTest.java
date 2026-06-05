package com.uniform.store.service.impl;

import com.uniform.store.entity.Coupon;
import com.uniform.store.entity.OrderCoupon;
import com.uniform.store.enums.CouponScope;
import com.uniform.store.enums.CouponStatus;
import com.uniform.store.enums.CouponType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.CartItemRepository;
import com.uniform.store.repository.CartRepository;
import com.uniform.store.repository.CouponRepository;
import com.uniform.store.repository.OrderCouponRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.CouponService.CartLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock CouponRepository couponRepository;
    @Mock OrderCouponRepository orderCouponRepository;
    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock com.uniform.store.service.PricingService pricingService;

    @InjectMocks CouponServiceImpl service;

    private static final long USER = 7L;

    private Coupon coupon(CouponType type, String value, CouponScope scope) {
        Coupon c = Coupon.builder()
                .code("SAVE").type(type).value(new BigDecimal(value)).scope(scope)
                .status(CouponStatus.ACTIVE).usedCount(0)
                .build();
        c.setId(1L);
        return c;
    }

    private CartLine line(Long productId, Long categoryId, String total) {
        return new CartLine(productId, categoryId, new BigDecimal(total));
    }

    private void stubLookup(Coupon c) {
        when(couponRepository.findByCode("SAVE")).thenReturn(Optional.of(c));
    }

    @Test
    void percentAll_appliesToWholeSubtotal() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("save", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("50000");
        verify(couponRepository).incrementUsage(1L);
    }

    @Test
    void fixedAll_subtractsAmount() {
        Coupon c = coupon(CouponType.FIXED, "30000", CouponScope.ALL);
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void fixedExceedingEligible_clampsToEligible() {
        Coupon c = coupon(CouponType.FIXED, "999999", CouponScope.ALL);
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void percentWithMaxDiscount_capped() {
        Coupon c = coupon(CouponType.PERCENT, "50", CouponScope.ALL);
        c.setMaxDiscountAmount(new BigDecimal("100000"));
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void categoryScope_discountsOnlyMatchingLines() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.CATEGORY);
        c.setCategoryIds(Set.of(2L));
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("SAVE", USER,
                List.of(line(1L, 2L, "300000"), line(2L, 9L, "200000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("30000"); // 10% of 300000
    }

    @Test
    void productScope_discountsOnlyListedProducts() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.PRODUCT);
        c.setProductIds(Set.of(1L));
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(1);

        var app = service.applyToOrder("SAVE", USER,
                List.of(line(1L, 2L, "300000"), line(2L, 9L, "200000")), new BigDecimal("500000"));

        assertThat(app.discountAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void minOrderNotMet_throws() {
        Coupon c = coupon(CouponType.FIXED, "30000", CouponScope.ALL);
        c.setMinOrderAmount(new BigDecimal("600000"));
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void expired_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setEndsAt(Instant.now().minusSeconds(3600));
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void notStarted_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setStartsAt(Instant.now().plusSeconds(3600));
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active yet");
    }

    @Test
    void disabled_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setStatus(CouponStatus.DISABLED);
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void globalUsageExhausted_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setMaxUses(5);
        c.setUsedCount(5);
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void perUserLimitReached_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setMaxUsesPerUser(1);
        stubLookup(c);
        when(orderCouponRepository.countByCouponAndUser(1L, USER)).thenReturn(1L);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void noEligibleItems_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.CATEGORY);
        c.setCategoryIds(Set.of(99L));
        stubLookup(c);

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER,
                List.of(line(1L, 2L, "300000")), new BigDecimal("300000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("eligible");
    }

    @Test
    void incrementRaceLost_throws() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        c.setMaxUses(5);
        stubLookup(c);
        when(couponRepository.incrementUsage(1L)).thenReturn(0); // someone else took the last use

        assertThatThrownBy(() -> service.applyToOrder("SAVE", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void invalidCode_throws() {
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyToOrder("nope", USER, List.of(line(1L, 2L, "500000")), new BigDecimal("500000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid coupon code");
    }

    @Test
    void releaseForOrder_decrementsAndDeletes() {
        Coupon c = coupon(CouponType.PERCENT, "10", CouponScope.ALL);
        OrderCoupon oc = OrderCoupon.builder().coupon(c).discountAmount(new BigDecimal("50000")).build();
        when(orderCouponRepository.findByOrderId(99L)).thenReturn(Optional.of(oc));
        lenient().when(couponRepository.decrementUsage(1L)).thenReturn(1);

        service.releaseForOrder(99L);

        verify(couponRepository).decrementUsage(1L);
        verify(orderCouponRepository).deleteByOrderId(99L);
    }

    @Test
    void releaseForOrder_noCoupon_noop() {
        when(orderCouponRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        service.releaseForOrder(99L);

        verify(orderCouponRepository).findByOrderId(99L);
    }
}
