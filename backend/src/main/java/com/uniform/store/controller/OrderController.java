package com.uniform.store.controller;

import com.uniform.store.dto.request.PlaceOrderRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.OrderResponse;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest req
    ) {
        return ApiResponse.ok("Order placed successfully", orderService.placeOrder(resolveUserId(userDetails), req));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.ok(orderService.getUserOrders(resolveUserId(userDetails), page));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(orderService.getOrderById(resolveUserId(userDetails), id));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
    }
}
