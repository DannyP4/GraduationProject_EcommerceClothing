package com.uniform.store.service.impl;

import com.uniform.store.config.ShippingProperties;
import com.uniform.store.enums.ShippingRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingServiceImplTest {

    ShippingProperties props;
    ShippingServiceImpl service;

    @BeforeEach
    void setup() {
        props = new ShippingProperties();
        service = new ShippingServiceImpl(props);
    }

    @Test
    void fee_perRegion_belowThreshold() {
        BigDecimal small = new BigDecimal("100000");
        assertThat(service.fee(ShippingRegion.NORTH, small)).isEqualByComparingTo("25000");
        assertThat(service.fee(ShippingRegion.CENTRAL, small)).isEqualByComparingTo("30000");
        assertThat(service.fee(ShippingRegion.SOUTH, small)).isEqualByComparingTo("35000");
    }

    @Test
    void fee_freeAtOrAboveThreshold() {
        assertThat(service.fee(ShippingRegion.SOUTH, new BigDecimal("500000"))).isEqualByComparingTo("0");
        assertThat(service.fee(ShippingRegion.NORTH, new BigDecimal("600000"))).isEqualByComparingTo("0");
    }

    @Test
    void fee_nullRegion_fallsBackToHighestTier() {
        assertThat(service.fee(null, new BigDecimal("100000"))).isEqualByComparingTo("35000");
    }

    @Test
    void fee_nullSubtotal_chargesBaseRate() {
        assertThat(service.fee(ShippingRegion.NORTH, null)).isEqualByComparingTo("25000");
    }

    @Test
    void freeThreshold_exposesConfiguredValue() {
        assertThat(service.freeThreshold()).isEqualByComparingTo("500000");
    }
}
