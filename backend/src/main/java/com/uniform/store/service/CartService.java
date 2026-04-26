package com.uniform.store.service;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddCartItemRequest req);
    CartResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest req);
    CartResponse removeItem(Long userId, Long cartItemId);
    CartResponse applyPromo(Long userId, String code);
    CartResponse removePromo(Long userId);
}
