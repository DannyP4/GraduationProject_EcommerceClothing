package com.uniform.store.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.shipping")
@Getter
@Setter
public class ShippingProperties {

    // Flat fee per region
    private BigDecimal north = new BigDecimal("25000");
    private BigDecimal central = new BigDecimal("30000");
    private BigDecimal south = new BigDecimal("35000");

    // >500000VND is free
    private BigDecimal freeThreshold = new BigDecimal("500000");
}
