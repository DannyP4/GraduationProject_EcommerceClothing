package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cart")
@Getter
@Setter
public class CartProperties {

    // Stock at-or-below this value (and > 0) is shown as LOW_STOCK on GET /cart.
    private int lowStockThreshold = 5;

    // Hard cap on per-line quantity.
    private int maxQuantityPerItem = 99;
}
