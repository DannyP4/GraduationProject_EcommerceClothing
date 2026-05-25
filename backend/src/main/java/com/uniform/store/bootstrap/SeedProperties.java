package com.uniform.store.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    private boolean enabled = true;
    private int productsCount = 50;
    private int customersCount = 60;
    private int ordersCount = 50;
    private int reviewsCount = 20;
}
