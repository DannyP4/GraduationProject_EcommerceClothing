package com.uniform.store.controller;

import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.OrderDetailDto;
import com.uniform.store.dto.response.OrderSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.PlaceOrderResponse;
import com.uniform.store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the current cart (COD, VNPAY, or STRIPE)")
    public ApiResponse<PlaceOrderResponse> placeOrder(Authentication authentication,
                                                      @Valid @RequestBody PlaceOrderRequest req,
                                                      HttpServletRequest http) {
        return ApiResponse.ok("Order placed",
                orderService.placeOrder(authentication.getName(), req, resolveClientIp(http)));
    }

    @GetMapping
    @Operation(summary = "List the current user's orders (newest first)")
    public ApiResponse<PageResponse<OrderSummaryDto>> listOrders(Authentication authentication,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ApiResponse.ok(orderService.listOrders(authentication.getName(), pageable));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order detail by order number")
    public ApiResponse<OrderDetailDto> getOrder(Authentication authentication,
                                                @PathVariable String orderNumber) {
        return ApiResponse.ok(orderService.getOrder(authentication.getName(), orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel a PENDING order (restores stock)")
    public ApiResponse<OrderDetailDto> cancelOrder(Authentication authentication,
                                                   @PathVariable String orderNumber) {
        return ApiResponse.ok("Order cancelled",
                orderService.cancelOrder(authentication.getName(), orderNumber));
    }

    private static String resolveClientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
