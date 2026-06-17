package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.dto.response.WishlistToggleResponse;
import com.uniform.store.i18n.RequestLocaleResolver;
import com.uniform.store.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final RequestLocaleResolver localeResolver;

    @GetMapping
    @Operation(summary = "List wishlisted products as cards")
    public ApiResponse<List<ProductSummaryDto>> list(
            Authentication auth,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(wishlistService.getWishlist(auth.getName(), locale));
    }

    @GetMapping("/ids")
    @Operation(summary = "List wishlisted product ids for hydrating heart state")
    public ApiResponse<List<Long>> ids(Authentication auth) {
        return ApiResponse.ok(wishlistService.getWishlistProductIds(auth.getName()));
    }

    @PostMapping("/{productId}/toggle")
    @Operation(summary = "Toggle a product in the wishlist")
    public ApiResponse<WishlistToggleResponse> toggle(Authentication auth, @PathVariable Long productId) {
        return ApiResponse.ok(wishlistService.toggle(auth.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from the wishlist")
    public ApiResponse<Void> remove(Authentication auth, @PathVariable Long productId) {
        wishlistService.remove(auth.getName(), productId);
        return ApiResponse.ok("Removed from wishlist", null);
    }

    @DeleteMapping
    @Operation(summary = "Clear the wishlist")
    public ApiResponse<Void> clear(Authentication auth) {
        wishlistService.clear(auth.getName());
        return ApiResponse.ok("Wishlist cleared", null);
    }
}
