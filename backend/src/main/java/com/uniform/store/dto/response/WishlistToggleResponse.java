package com.uniform.store.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistToggleResponse {

    private final Long productId;
    private final boolean wishlisted;
}
