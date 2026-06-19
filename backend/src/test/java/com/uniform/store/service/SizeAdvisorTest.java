package com.uniform.store.service;

import com.uniform.store.service.SizeAdvisor.SizeAdvice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SizeAdvisorTest {

    @Test
    void midRange_170_65_isLthenXL() {
        SizeAdvice a = SizeAdvisor.recommend(170, 65);
        assertThat(a.recommended()).isEqualTo("L");
        assertThat(a.comfortable()).isEqualTo("XL");
    }

    @Test
    void light_isSthenM() {
        SizeAdvice a = SizeAdvisor.recommend(160, 50);
        assertThat(a.recommended()).isEqualTo("S");
        assertThat(a.comfortable()).isEqualTo("M");
    }

    @Test
    void heavy_isXLthenXXL() {
        SizeAdvice a = SizeAdvisor.recommend(175, 75);
        assertThat(a.recommended()).isEqualTo("XL");
        assertThat(a.comfortable()).isEqualTo("XXL");
    }

    @Test
    void veryTall_bumpsUpForLength() {
        assertThat(SizeAdvisor.recommend(190, 65).recommended()).isEqualTo("XL");
    }

    @Test
    void weightBandBoundaries() {
        assertThat(SizeAdvisor.recommend(170, 52).recommended()).isEqualTo("S");
        assertThat(SizeAdvisor.recommend(170, 53).recommended()).isEqualTo("M");
        assertThat(SizeAdvisor.recommend(170, 68).recommended()).isEqualTo("L");
        assertThat(SizeAdvisor.recommend(170, 69).recommended()).isEqualTo("XL");
    }

    @Test
    void topSize_comfortableCaps() {
        SizeAdvice a = SizeAdvisor.recommend(170, 95);
        assertThat(a.recommended()).isEqualTo("XXXL");
        assertThat(a.comfortable()).isEqualTo("XXXL");
    }
}
