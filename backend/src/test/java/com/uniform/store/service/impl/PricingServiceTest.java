package com.uniform.store.service.impl;

import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.SaleType;
import com.uniform.store.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-04T00:00:00Z");

    private final PricingService pricing = new PricingService();

    private Product product(BigDecimal base, SaleType type, BigDecimal value, Instant start, Instant end) {
        return Product.builder()
                .basePrice(base)
                .saleType(type)
                .saleValue(value)
                .saleStartsAt(start)
                .saleEndsAt(end)
                .build();
    }

    private ProductVariant variant(BigDecimal override) {
        return ProductVariant.builder().priceOverride(override).build();
    }

    @Test
    void noSale_effectiveEqualsBase() {
        Product p = product(new BigDecimal("250000"), null, null, null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isFalse();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("250000");
        assertThat(ep.originalPrice()).isEqualByComparingTo("250000");
        assertThat(ep.discountPercent()).isNull();
    }

    @Test
    void variantOverride_winsOverBase_whenNoSale() {
        Product p = product(new BigDecimal("250000"), null, null, null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, variant(new BigDecimal("300000")), NOW);

        assertThat(ep.originalPrice()).isEqualByComparingTo("300000");
        assertThat(ep.effectivePrice()).isEqualByComparingTo("300000");
        assertThat(ep.onSale()).isFalse();
    }

    @Test
    void percentSale_active_discountsAndReportsPercent() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, new BigDecimal("30"), null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isTrue();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("175000");
        assertThat(ep.discountPercent()).isEqualTo(30);
    }

    @Test
    void fixedSale_active_subtractsAmount() {
        Product p = product(new BigDecimal("250000"), SaleType.FIXED, new BigDecimal("50000"), null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isTrue();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("200000");
        assertThat(ep.discountPercent()).isEqualTo(20);
    }

    @Test
    void percentSale_appliesToVariantOverride_notBase() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, new BigDecimal("30"), null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, variant(new BigDecimal("300000")), NOW);

        assertThat(ep.originalPrice()).isEqualByComparingTo("300000");
        assertThat(ep.effectivePrice()).isEqualByComparingTo("210000");
        assertThat(ep.discountPercent()).isEqualTo(30);
    }

    @Test
    void saleNotStarted_isInactive() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, new BigDecimal("30"),
                NOW.plusSeconds(3600), null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isFalse();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("250000");
    }

    @Test
    void saleEnded_isInactive() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, new BigDecimal("30"),
                null, NOW.minusSeconds(3600));

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isFalse();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("250000");
    }

    @Test
    void openEndedWindow_isActive() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, new BigDecimal("30"),
                NOW.minusSeconds(3600), NOW.plusSeconds(3600));

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isTrue();
        assertThat(ep.effectivePrice()).isEqualByComparingTo("175000");
    }

    @Test
    void fixedSaleExceedingBase_clampsToZero() {
        Product p = product(new BigDecimal("100000"), SaleType.FIXED, new BigDecimal("150000"), null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.effectivePrice()).isEqualByComparingTo("0");
        assertThat(ep.discountPercent()).isEqualTo(100);
    }

    @Test
    void percentSale_roundsToWholeDong() {
        Product p = product(new BigDecimal("99999"), SaleType.PERCENT, new BigDecimal("10"), null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.effectivePrice()).isEqualByComparingTo("89999");
        assertThat(ep.discountPercent()).isEqualTo(10);
    }

    @Test
    void zeroSaleValue_isInactive() {
        Product p = product(new BigDecimal("250000"), SaleType.PERCENT, BigDecimal.ZERO, null, null);

        PricingService.EffectivePrice ep = pricing.resolve(p, null, NOW);

        assertThat(ep.onSale()).isFalse();
    }
}
