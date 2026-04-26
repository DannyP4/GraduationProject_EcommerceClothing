package com.uniform.store.controller;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.ApplyPromoRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CartResponse;
import com.uniform.store.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.uniform.store.repository.UserRepository;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(cartService.getCart(resolveUserId(userDetails)));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddCartItemRequest req
    ) {
        return ApiResponse.ok(cartService.addItem(resolveUserId(userDetails), req));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest req
    ) {
        return ApiResponse.ok(cartService.updateItem(resolveUserId(userDetails), id, req));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(cartService.removeItem(resolveUserId(userDetails), id));
    }

    @PostMapping("/promo")
    public ApiResponse<CartResponse> applyPromo(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyPromoRequest req
    ) {
        return ApiResponse.ok(cartService.applyPromo(resolveUserId(userDetails), req.getCode()));
    }

    @DeleteMapping("/promo")
    public ApiResponse<CartResponse> removePromo(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(cartService.removePromo(resolveUserId(userDetails)));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
    }
}
