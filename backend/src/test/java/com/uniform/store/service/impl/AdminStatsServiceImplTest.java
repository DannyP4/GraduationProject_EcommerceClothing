package com.uniform.store.service.impl;

import com.uniform.store.config.CartProperties;
import com.uniform.store.dto.response.DashboardOpsDto;
import com.uniform.store.dto.response.OrdersByStatusDto;
import com.uniform.store.dto.response.RevenueBucketDto;
import com.uniform.store.dto.response.StatsSummaryDto;
import com.uniform.store.dto.response.TopCustomerDto;
import com.uniform.store.dto.response.TopProductDto;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.StatsGranularity;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.OrderRepository;
import com.uniform.store.repository.ProductVariantRepository;
import com.uniform.store.repository.StatsRepository;
import com.uniform.store.repository.StatsRepository.RevenueTotals;
import com.uniform.store.service.impl.AdminStatsServiceImpl.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock StatsRepository statsRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductVariantRepository productVariantRepository;
    CartProperties cartProperties;

    AdminStatsServiceImpl service;

    @BeforeEach
    void setup() {
        cartProperties = new CartProperties();
        service = new AdminStatsServiceImpl(statsRepository, orderRepository, productVariantRepository, cartProperties);
    }

    @Test
    void resolveRange_bothNull_defaultsToLast30Days() {
        Range r = AdminStatsServiceImpl.resolveRange(null, null);
        LocalDate today = LocalDate.now(ZONE);
        assertThat(r.to()).isEqualTo(today);
        assertThat(r.from()).isEqualTo(today.minusDays(29));
        long days = ChronoUnit.DAYS.between(r.from(), r.to()) + 1;
        assertThat(days).isEqualTo(30);
    }

    @Test
    void resolveRange_fromAfterTo_throws() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate to = LocalDate.of(2026, 5, 1);
        assertThatThrownBy(() -> AdminStatsServiceImpl.resolveRange(from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("on or before");
    }

    @Test
    void resolveRange_tooLarge_throws() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> AdminStatsServiceImpl.resolveRange(from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Range too large");
    }

    @Test
    void resolveRange_previousPeriodHasSameDuration() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 30);
        Range r = AdminStatsServiceImpl.resolveRange(from, to);

        long curMs = r.toInst().toEpochMilli() - r.fromInst().toEpochMilli();
        long prevMs = r.prevToInst().toEpochMilli() - r.prevFromInst().toEpochMilli();
        assertThat(prevMs).isEqualTo(curMs);
        assertThat(r.prevToInst()).isEqualTo(r.fromInst());
    }

    @Test
    void pctChange_previousZero_returnsNull() {
        assertThat(AdminStatsServiceImpl.pctChange(new BigDecimal("100"), BigDecimal.ZERO)).isNull();
        assertThat(AdminStatsServiceImpl.pctChange(BigDecimal.ZERO, BigDecimal.ZERO)).isNull();
        assertThat(AdminStatsServiceImpl.pctChange(BigDecimal.ZERO, null)).isNull();
    }

    @Test
    void pctChange_positiveGrowth() {
        Double pct = AdminStatsServiceImpl.pctChange(new BigDecimal("150"), new BigDecimal("100"));
        assertThat(pct).isNotNull();
        assertThat(pct).isCloseTo(50.0, within(0.01));
    }

    @Test
    void pctChange_negative() {
        Double pct = AdminStatsServiceImpl.pctChange(new BigDecimal("80"), new BigDecimal("100"));
        assertThat(pct).isNotNull();
        assertThat(pct).isCloseTo(-20.0, within(0.01));
    }

    @Test
    void summary_computesAovAndChanges() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 30);

        when(statsRepository.revenueTotals(any(Instant.class), any(Instant.class)))
                .thenReturn(new RevenueTotals(new BigDecimal("1000000"), 5L))
                .thenReturn(new RevenueTotals(new BigDecimal("500000"), 2L));
        when(statsRepository.countNewCustomers(any(Instant.class), any(Instant.class)))
                .thenReturn(10L)
                .thenReturn(5L);

        StatsSummaryDto summary = service.summary(from, to);

        assertThat(summary.getFrom()).isEqualTo(from);
        assertThat(summary.getTo()).isEqualTo(to);
        assertThat(summary.getCurrent().getRevenue()).isEqualByComparingTo("1000000");
        assertThat(summary.getCurrent().getOrders()).isEqualTo(5);
        assertThat(summary.getCurrent().getAvgOrderValue()).isEqualByComparingTo("200000");
        assertThat(summary.getCurrent().getNewCustomers()).isEqualTo(10);
        assertThat(summary.getPrevious().getRevenue()).isEqualByComparingTo("500000");
        assertThat(summary.getPrevious().getAvgOrderValue()).isEqualByComparingTo("250000");
        assertThat(summary.getChanges().getRevenuePct()).isCloseTo(100.0, within(0.01));
        assertThat(summary.getChanges().getOrdersPct()).isCloseTo(150.0, within(0.01));
        assertThat(summary.getChanges().getNewCustomersPct()).isCloseTo(100.0, within(0.01));
    }

    @Test
    void summary_previousZero_changesAreNull() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 30);

        when(statsRepository.revenueTotals(any(Instant.class), any(Instant.class)))
                .thenReturn(new RevenueTotals(new BigDecimal("1000000"), 5L))
                .thenReturn(new RevenueTotals(BigDecimal.ZERO, 0L));
        when(statsRepository.countNewCustomers(any(Instant.class), any(Instant.class)))
                .thenReturn(10L).thenReturn(0L);

        StatsSummaryDto summary = service.summary(from, to);

        assertThat(summary.getChanges().getRevenuePct()).isNull();
        assertThat(summary.getChanges().getOrdersPct()).isNull();
        assertThat(summary.getChanges().getAvgOrderValuePct()).isNull();
        assertThat(summary.getChanges().getNewCustomersPct()).isNull();
    }

    @Test
    void summary_zeroOrders_aovIsZero() {
        when(statsRepository.revenueTotals(any(Instant.class), any(Instant.class)))
                .thenReturn(new RevenueTotals(BigDecimal.ZERO, 0L))
                .thenReturn(new RevenueTotals(BigDecimal.ZERO, 0L));
        when(statsRepository.countNewCustomers(any(Instant.class), any(Instant.class)))
                .thenReturn(0L).thenReturn(0L);

        StatsSummaryDto summary = service.summary(null, null);
        assertThat(summary.getCurrent().getAvgOrderValue()).isEqualByComparingTo("0");
    }

    @Test
    void revenueTimeSeries_nullGranularity_defaultsToDay() {
        when(statsRepository.revenueTimeSeries(any(), any(), any()))
                .thenReturn(List.of());

        service.revenueTimeSeries(null, null, null);

        ArgumentCaptor<StatsGranularity> cap = ArgumentCaptor.forClass(StatsGranularity.class);
        verify(statsRepository).revenueTimeSeries(any(), any(), cap.capture());
        assertThat(cap.getValue()).isEqualTo(StatsGranularity.DAY);
    }

    @Test
    void revenueTimeSeries_passesGranularityToRepo() {
        when(statsRepository.revenueTimeSeries(any(), any(), any()))
                .thenReturn(List.of(RevenueBucketDto.builder().bucket("2026-W22").revenue(new BigDecimal("100000")).orderCount(3).build()));

        List<RevenueBucketDto> out = service.revenueTimeSeries(null, null, StatsGranularity.WEEK);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getBucket()).isEqualTo("2026-W22");
        ArgumentCaptor<StatsGranularity> cap = ArgumentCaptor.forClass(StatsGranularity.class);
        verify(statsRepository).revenueTimeSeries(any(), any(), cap.capture());
        assertThat(cap.getValue()).isEqualTo(StatsGranularity.WEEK);
    }

    @Test
    void ordersByStatus_zeroFillsMissingStatuses() {
        when(statsRepository.ordersByStatus(any(), any()))
                .thenReturn(List.of(
                        OrdersByStatusDto.builder().status(OrderStatus.PAID).count(3).revenue(new BigDecimal("300000")).build(),
                        OrdersByStatusDto.builder().status(OrderStatus.DELIVERED).count(2).revenue(new BigDecimal("200000")).build()
                ));

        List<OrdersByStatusDto> out = service.ordersByStatus(null, null);

        assertThat(out).hasSize(OrderStatus.values().length);
        assertThat(out.stream().filter(d -> d.getStatus() == OrderStatus.PAID).findFirst().orElseThrow().getCount())
                .isEqualTo(3);
        assertThat(out.stream().filter(d -> d.getStatus() == OrderStatus.PENDING).findFirst().orElseThrow().getCount())
                .isZero();
        assertThat(out.stream().filter(d -> d.getStatus() == OrderStatus.PENDING).findFirst().orElseThrow().getRevenue())
                .isEqualByComparingTo("0");
    }

    @Test
    void topProducts_clampsLimit() {
        when(statsRepository.topProducts(any(), any(), anyInt()))
                .thenReturn(List.of(TopProductDto.builder().productId(1L).productName("P").unitsSold(10).revenue(new BigDecimal("100")).build()));

        service.topProducts(null, null, 0);
        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(statsRepository).topProducts(any(), any(), cap.capture());
        assertThat(cap.getValue()).isEqualTo(10);
    }

    @Test
    void topProducts_clampsLimitToMax() {
        when(statsRepository.topProducts(any(), any(), anyInt()))
                .thenReturn(List.of());

        service.topProducts(null, null, 9999);
        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(statsRepository).topProducts(any(), any(), cap.capture());
        assertThat(cap.getValue()).isEqualTo(50);
    }

    @Test
    void topCustomers_defaultLimit5() {
        when(statsRepository.topCustomers(any(), any(), anyInt()))
                .thenReturn(List.of(TopCustomerDto.builder().userId(1L).email("a@a").fullName("A").orderCount(1).totalSpent(BigDecimal.ONE).build()));

        service.topCustomers(null, null, 0);
        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(statsRepository).topCustomers(any(), any(), cap.capture());
        assertThat(cap.getValue()).isEqualTo(5);
    }

    @Test
    void ops_returnsOpenOrdersAndLowStockWithConfiguredThreshold() {
        when(orderRepository.countByStatusIn(any())).thenReturn(7L);
        when(productVariantRepository.countByIsActiveTrueAndStockQuantityLessThanEqual(anyInt())).thenReturn(3L);

        DashboardOpsDto ops = service.ops();

        assertThat(ops.getOpenOrders()).isEqualTo(7);
        assertThat(ops.getLowStock()).isEqualTo(3);
        assertThat(ops.getLowStockThreshold()).isEqualTo(5); // CartProperties default
    }

    @Test
    void ops_passesConfiguredThresholdToVariantQuery() {
        cartProperties.setLowStockThreshold(12);
        when(orderRepository.countByStatusIn(any())).thenReturn(0L);
        when(productVariantRepository.countByIsActiveTrueAndStockQuantityLessThanEqual(anyInt())).thenReturn(0L);

        service.ops();

        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(productVariantRepository).countByIsActiveTrueAndStockQuantityLessThanEqual(cap.capture());
        assertThat(cap.getValue()).isEqualTo(12);
    }

    @Test
    void ops_countsOnlyOpenStatuses() {
        when(orderRepository.countByStatusIn(any())).thenReturn(0L);
        when(productVariantRepository.countByIsActiveTrueAndStockQuantityLessThanEqual(anyInt())).thenReturn(0L);

        service.ops();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<OrderStatus>> cap = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(orderRepository).countByStatusIn(cap.capture());
        assertThat(cap.getValue())
                .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.PROCESSING);
    }
}
