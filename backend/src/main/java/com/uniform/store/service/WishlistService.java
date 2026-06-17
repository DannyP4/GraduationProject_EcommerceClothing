package com.uniform.store.service;

import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.dto.response.WishlistToggleResponse;

import java.util.List;

public interface WishlistService {

    List<ProductSummaryDto> getWishlist(String userEmail, String locale);

    List<Long> getWishlistProductIds(String userEmail);

    WishlistToggleResponse toggle(String userEmail, Long productId);

    void remove(String userEmail, Long productId);

    void clear(String userEmail);
}
