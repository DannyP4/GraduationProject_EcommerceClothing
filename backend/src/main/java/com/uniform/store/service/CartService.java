package com.uniform.store.service;

import com.uniform.store.dto.request.AddCartItemRequest;
import com.uniform.store.dto.request.MergeCartRequest;
import com.uniform.store.dto.request.UpdateCartItemRequest;
import com.uniform.store.dto.response.CartDto;

public interface CartService {

    CartDto getCart(String email);

    CartDto addItem(String email, AddCartItemRequest req);

    CartDto updateItem(String email, Long itemId, UpdateCartItemRequest req);

    CartDto removeItem(String email, Long itemId);

    CartDto clearCart(String email);

    CartDto mergeCart(String email, MergeCartRequest req);
}
