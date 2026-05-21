package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FxServiceImplTest {

    private final FxServiceImpl fx = new FxServiceImpl(new BigDecimal("0.0000385"), "TEST_FIXED");

    @Test
    void quoteVndToUsd_convertsAtFixedRate_roundsHalfUpToCents() {
        FxQuote quote = fx.quoteVndToUsd(new BigDecimal("250000"));

        assertThat(quote.convertedCurrency()).as("target currency").isEqualTo("USD");
        assertThat(quote.originalCurrency()).as("source currency").isEqualTo("VND");
        assertThat(quote.convertedAmount())
                .as("250000 * 0.0000385 = 9.625 → HALF_UP → 9.63")
                .isEqualByComparingTo("9.63");
        assertThat(quote.convertedAmountInMinorUnits())
                .as("Stripe minor units = cents")
                .isEqualTo(963L);
        assertThat(quote.rate()).isEqualByComparingTo("0.0000385");
        assertThat(quote.provider()).isEqualTo("TEST_FIXED");
    }

    @Test
    void quoteVndToUsd_rejectsZeroOrNullAmount() {
        assertThatThrownBy(() -> fx.quoteVndToUsd(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fx.quoteVndToUsd(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fx.quoteVndToUsd(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
