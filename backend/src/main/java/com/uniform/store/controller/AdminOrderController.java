package com.uniform.store.controller;

import com.uniform.store.dto.request.AdminOrderFilter;
import com.uniform.store.dto.request.CancelOrderRequest;
import com.uniform.store.dto.request.TransitionOrderRequest;
import com.uniform.store.dto.response.AdminOrderDetailDto;
import com.uniform.store.dto.response.AdminOrderSummaryDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.enums.OrderStatus;
import com.uniform.store.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "List orders with optional status / date-range / search filter")
    public ApiResponse<PageResponse<AdminOrderSummaryDto>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "placedAt"));

        AdminOrderFilter filter = AdminOrderFilter.builder()
                .status(status)
                .search(search)
                .placedFrom(from == null ? null : from.atStartOfDay(ZONE).toInstant())
                .placedTo(to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant())
                .build();

        return ApiResponse.ok(adminOrderService.listOrders(filter, pageable));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Admin view of an order by orderNumber — includes customer + allowed transitions")
    public ApiResponse<AdminOrderDetailDto> get(@PathVariable String orderNumber) {
        return ApiResponse.ok(adminOrderService.getOrder(orderNumber));
    }

    @PatchMapping("/{orderNumber}/transition")
    @Operation(summary = "Move order to the next valid status (state machine enforced)")
    public ApiResponse<AdminOrderDetailDto> transition(Authentication auth,
                                                       @PathVariable String orderNumber,
                                                       @Valid @RequestBody TransitionOrderRequest req) {
        return ApiResponse.ok("Order status updated",
                adminOrderService.transitionOrder(orderNumber, req.getTargetStatus(), req.getNote(), auth.getName()));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Admin override cancel (PENDING / PAID / PROCESSING) — restores stock; captured payment requires manual refund")
    public ApiResponse<AdminOrderDetailDto> cancel(Authentication auth,
                                                   @PathVariable String orderNumber,
                                                   @RequestBody(required = false) @Valid CancelOrderRequest req) {
        String reason = req == null ? null : req.getReason();
        return ApiResponse.ok("Order cancelled",
                adminOrderService.cancelOrder(orderNumber, reason, auth.getName()));
    }
}
