package com.uniform.store.service.impl;

import com.uniform.store.dto.response.OrdersByStatusDto;
import com.uniform.store.dto.response.PaymentBreakdownDto;
import com.uniform.store.dto.response.RevenueBucketDto;
import com.uniform.store.dto.response.StatsSummaryDto;
import com.uniform.store.dto.response.TopCustomerDto;
import com.uniform.store.dto.response.TopProductDto;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.enums.StatsGranularity;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.StatsRepository;
import com.uniform.store.repository.StatsRepository.RevenueTotals;
import com.uniform.store.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsServiceImpl implements AdminStatsService {

    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int DEFAULT_RANGE_DAYS = 30;
    static final int MAX_RANGE_DAYS = 366;

    private final StatsRepository statsRepository;

    @Override
    public StatsSummaryDto summary(LocalDate from, LocalDate to) {
        Range r = resolveRange(from, to);

        RevenueTotals curRev = statsRepository.revenueTotals(r.fromInst, r.toInst);
        RevenueTotals prevRev = statsRepository.revenueTotals(r.prevFromInst, r.prevToInst);
        long curNew = statsRepository.countNewCustomers(r.fromInst, r.toInst);
        long prevNew = statsRepository.countNewCustomers(r.prevFromInst, r.prevToInst);

        BigDecimal curAov = avg(curRev.revenue(), curRev.orders());
        BigDecimal prevAov = avg(prevRev.revenue(), prevRev.orders());

        StatsSummaryDto.Period current = StatsSummaryDto.Period.builder()
                .revenue(curRev.revenue())
                .orders(curRev.orders())
                .avgOrderValue(curAov)
                .newCustomers(curNew)
                .build();

        StatsSummaryDto.Period previous = StatsSummaryDto.Period.builder()
                .revenue(prevRev.revenue())
                .orders(prevRev.orders())
                .avgOrderValue(prevAov)
                .newCustomers(prevNew)
                .build();

        StatsSummaryDto.Changes changes = StatsSummaryDto.Changes.builder()
                .revenuePct(pctChange(curRev.revenue(), prevRev.revenue()))
                .ordersPct(pctChange(BigDecimal.valueOf(curRev.orders()), BigDecimal.valueOf(prevRev.orders())))
                .avgOrderValuePct(pctChange(curAov, prevAov))
                .newCustomersPct(pctChange(BigDecimal.valueOf(curNew), BigDecimal.valueOf(prevNew)))
                .build();

        return StatsSummaryDto.builder()
                .from(r.from)
                .to(r.to)
                .current(current)
                .previous(previous)
                .changes(changes)
                .build();
    }

    @Override
    public List<RevenueBucketDto> revenueTimeSeries(LocalDate from, LocalDate to, StatsGranularity granularity) {
        Range r = resolveRange(from, to);
        StatsGranularity g = granularity == null ? StatsGranularity.DAY : granularity;
        return statsRepository.revenueTimeSeries(r.fromInst, r.toInst, g);
    }

    @Override
    public List<PaymentBreakdownDto> paymentBreakdown(LocalDate from, LocalDate to) {
        Range r = resolveRange(from, to);
        return statsRepository.paymentBreakdown(r.fromInst, r.toInst);
    }

    @Override
    public List<OrdersByStatusDto> ordersByStatus(LocalDate from, LocalDate to) {
        Range r = resolveRange(from, to);
        List<OrdersByStatusDto> found = statsRepository.ordersByStatus(r.fromInst, r.toInst);
        Map<OrderStatus, OrdersByStatusDto> byStatus = new HashMap<>();
        for (OrdersByStatusDto d : found) byStatus.put(d.getStatus(), d);
        return java.util.Arrays.stream(OrderStatus.values())
                .map(s -> byStatus.getOrDefault(s, OrdersByStatusDto.builder()
                        .status(s).count(0).revenue(BigDecimal.ZERO).build()))
                .toList();
    }

    @Override
    public List<TopProductDto> topProducts(LocalDate from, LocalDate to, int limit) {
        Range r = resolveRange(from, to);
        return statsRepository.topProducts(r.fromInst, r.toInst, clampLimit(limit, 10, 50));
    }

    @Override
    public List<TopCustomerDto> topCustomers(LocalDate from, LocalDate to, int limit) {
        Range r = resolveRange(from, to);
        return statsRepository.topCustomers(r.fromInst, r.toInst, clampLimit(limit, 5, 50));
    }

    static Range resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate effFrom = from != null ? from : today.minusDays(DEFAULT_RANGE_DAYS - 1L);
        LocalDate effTo = to != null ? to : today;

        if (effFrom.isAfter(effTo)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        long days = effTo.toEpochDay() - effFrom.toEpochDay() + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BadRequestException("Range too large (max " + MAX_RANGE_DAYS + " days)");
        }

        Instant fromInst = effFrom.atStartOfDay(ZONE).toInstant();
        Instant toInst = effTo.plusDays(1).atStartOfDay(ZONE).toInstant();
        long durationMs = toInst.toEpochMilli() - fromInst.toEpochMilli();
        Instant prevFromInst = Instant.ofEpochMilli(fromInst.toEpochMilli() - durationMs);
        Instant prevToInst = fromInst;

        return new Range(effFrom, effTo, fromInst, toInst, prevFromInst, prevToInst);
    }

    private static BigDecimal avg(BigDecimal revenue, long orders) {
        if (orders <= 0) return BigDecimal.ZERO;
        return revenue.divide(BigDecimal.valueOf(orders), 4, RoundingMode.HALF_UP);
    }

    static Double pctChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private static int clampLimit(int requested, int defaultLimit, int max) {
        if (requested <= 0) return defaultLimit;
        return Math.min(requested, max);
    }

    record Range(LocalDate from, LocalDate to,
                 Instant fromInst, Instant toInst,
                 Instant prevFromInst, Instant prevToInst) {}
}
