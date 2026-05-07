package com.uniform.store.controller;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.MergeCartRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CartDto;
import com.uniform.store.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ApiResponse<CartDto> getCart(Authentication authentication) {
        return ApiResponse.ok(cartService.getCart(authentication.getName()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a variant to the cart (or increment if already present)")
    public ApiResponse<CartDto> addItem(Authentication authentication,
                                        @Valid @RequestBody AddCartItemRequest req) {
        return ApiResponse.ok("Item added", cartService.addItem(authentication.getName(), req));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update item quantity (quantity = 0 deletes the item)")
    public ApiResponse<CartDto> updateItem(Authentication authentication,
                                           @PathVariable Long itemId,
                                           @Valid @RequestBody UpdateCartItemRequest req) {
        return ApiResponse.ok(cartService.updateItem(authentication.getName(), itemId, req));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a single item from the cart")
    public ApiResponse<CartDto> removeItem(Authentication authentication,
                                           @PathVariable Long itemId) {
        return ApiResponse.ok("Item removed", cartService.removeItem(authentication.getName(), itemId));
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from the cart")
    public ApiResponse<CartDto> clearCart(Authentication authentication) {
        return ApiResponse.ok("Cart cleared", cartService.clearCart(authentication.getName()));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a guest cart (from FE localStorage) into the user's cart")
    public ApiResponse<CartDto> mergeCart(Authentication authentication,
                                          @Valid @RequestBody MergeCartRequest req) {
        return ApiResponse.ok("Cart merged", cartService.mergeCart(authentication.getName(), req));
    }
}
