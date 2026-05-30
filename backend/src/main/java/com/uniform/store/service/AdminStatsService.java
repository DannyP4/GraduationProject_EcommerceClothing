package com.uniform.store.service;

import com.uniform.store.dto.response.OrdersByStatusDto;
import com.uniform.store.dto.response.PaymentBreakdownDto;
import com.uniform.store.dto.response.RevenueBucketDto;
import com.uniform.store.dto.response.StatsSummaryDto;
import com.uniform.store.dto.response.TopCustomerDto;
import com.uniform.store.dto.response.TopProductDto;
import com.uniform.store.enums.StatsGranularity;

import java.time.LocalDate;
import java.util.List;

public interface AdminStatsService {

    StatsSummaryDto summary(LocalDate from, LocalDate to);

    List<RevenueBucketDto> revenueTimeSeries(LocalDate from, LocalDate to, StatsGranularity granularity);

    List<PaymentBreakdownDto> paymentBreakdown(LocalDate from, LocalDate to);

    List<OrdersByStatusDto> ordersByStatus(LocalDate from, LocalDate to);

    List<TopProductDto> topProducts(LocalDate from, LocalDate to, int limit);

    List<TopCustomerDto> topCustomers(LocalDate from, LocalDate to, int limit);
}
