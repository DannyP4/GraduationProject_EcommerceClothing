package com.uniform.store.service.impl;

import com.uniform.store.config.GhnProperties;
import com.uniform.store.config.ShippingProperties;
import com.uniform.store.enums.ShippingRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    ShippingProperties props;
    GhnProperties ghnProps;
    @Mock GhnClient ghnClient;
    ShippingServiceImpl service;

    @BeforeEach
    void setup() {
        props = new ShippingProperties();
        ghnProps = new GhnProperties();
        service = new ShippingServiceImpl(props, ghnProps, ghnClient);
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

    @Test
    void ghnFee_freeAtOrAboveThreshold() {
        assertThat(service.ghnFee(100, "1A", 2, new BigDecimal("500000"))).isEqualByComparingTo("0");
    }

    @Test
    void ghnFee_usesGhnTotalWhenAvailable() {
        when(ghnClient.calculateFee(anyInt(), anyString(), anyInt()))
                .thenReturn(Optional.of(new BigDecimal("42000")));
        assertThat(service.ghnFee(100, "1A", 2, new BigDecimal("100000"))).isEqualByComparingTo("42000");
    }

    @Test
    void ghnFee_fallsBackToFlatRateWhenGhnUnavailable() {
        when(ghnClient.calculateFee(anyInt(), anyString(), anyInt())).thenReturn(Optional.empty());
        assertThat(service.ghnFee(100, "1A", 2, new BigDecimal("100000"))).isEqualByComparingTo("35000");
    }

    @Test
    void ghnFee_missingLocation_fallsBackToFlatRate() {
        assertThat(service.ghnFee(null, null, 1, new BigDecimal("100000"))).isEqualByComparingTo("35000");
    }
}
