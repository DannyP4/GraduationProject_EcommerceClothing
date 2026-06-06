package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.DashboardOpsDto;
import com.uniform.store.dto.response.OrdersByStatusDto;
import com.uniform.store.dto.response.PaymentBreakdownDto;
import com.uniform.store.dto.response.RevenueBucketDto;
import com.uniform.store.dto.response.StatsSummaryDto;
import com.uniform.store.dto.response.TopCustomerDto;
import com.uniform.store.dto.response.TopProductDto;
import com.uniform.store.enums.StatsGranularity;
import com.uniform.store.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin Stats")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/summary")
    @Operation(summary = "KPI cards with previous-period comparison (revenue, orders, AOV, new customers)")
    public ApiResponse<StatsSummaryDto> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminStatsService.summary(from, to));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue time series bucketed by day, week, or month")
    public ApiResponse<List<RevenueBucketDto>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "DAY") StatsGranularity granularity) {
        return ApiResponse.ok(adminStatsService.revenueTimeSeries(from, to, granularity));
    }

    @GetMapping("/payment-breakdown")
    @Operation(summary = "Revenue breakdown by payment provider (CAPTURED payments on revenue-status orders)")
    public ApiResponse<List<PaymentBreakdownDto>> paymentBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminStatsService.paymentBreakdown(from, to));
    }

    @GetMapping("/orders-by-status")
    @Operation(summary = "Count and revenue of orders grouped by status (all 7 statuses, zero-filled)")
    public ApiResponse<List<OrdersByStatusDto>> ordersByStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminStatsService.ordersByStatus(from, to));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top N products by revenue across all variants")
    public ApiResponse<List<TopProductDto>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.ok(adminStatsService.topProducts(from, to, limit));
    }

    @GetMapping("/top-customers")
    @Operation(summary = "Top N customers by total spent")
    public ApiResponse<List<TopCustomerDto>> topCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ApiResponse.ok(adminStatsService.topCustomers(from, to, limit));
    }

    @GetMapping("/ops")
    @Operation(summary = "Operational snapshot for the dashboard: open-order and low-stock variant counts")
    public ApiResponse<DashboardOpsDto> ops() {
        return ApiResponse.ok(adminStatsService.ops());
    }
}
