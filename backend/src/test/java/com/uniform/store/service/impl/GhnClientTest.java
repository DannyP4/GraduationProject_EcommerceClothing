package com.uniform.store.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GhnClientTest {

    @Test
    void isMockProvinceName_flagsSandboxJunk() {
        assertThat(GhnClient.isMockProvinceName("Test - Alert - Tỉnh - 001")).isTrue();
        assertThat(GhnClient.isMockProvinceName("Hà Nội 02")).isTrue();
        assertThat(GhnClient.isMockProvinceName(null)).isTrue();
        assertThat(GhnClient.isMockProvinceName("   ")).isTrue();
    }

    @Test
    void isMockProvinceName_keepsRealProvinces() {
        assertThat(GhnClient.isMockProvinceName("Hà Nội")).isFalse();
        assertThat(GhnClient.isMockProvinceName("Hồ Chí Minh")).isFalse();
        assertThat(GhnClient.isMockProvinceName("Bà Rịa - Vũng Tàu")).isFalse();
        assertThat(GhnClient.isMockProvinceName("Lào Cai")).isFalse();
    }
}
