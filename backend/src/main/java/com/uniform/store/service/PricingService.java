package com.uniform.store.service;

import com.uniform.store.entity.Product;
import com.uniform.store.entity.ProductVariant;
import com.uniform.store.enums.SaleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Component
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public EffectivePrice resolve(Product product, ProductVariant variant, Instant now) {
        BigDecimal base = (variant != null && variant.getPriceOverride() != null)
                ? variant.getPriceOverride()
                : product.getBasePrice();

        if (!isSaleActive(product, now)) {
            return new EffectivePrice(base, base, false, null);
        }

        BigDecimal effective = applyDiscount(base, product.getSaleType(), product.getSaleValue());
        if (effective.compareTo(BigDecimal.ZERO) < 0) effective = BigDecimal.ZERO;
        if (effective.compareTo(base) > 0) effective = base;

        boolean onSale = effective.compareTo(base) < 0;
        Integer pct = onSale ? percentOff(base, effective) : null;
        return new EffectivePrice(base, effective, onSale, pct);
    }

    public boolean isSaleActive(Product product, Instant now) {
        SaleType type = product.getSaleType();
        BigDecimal value = product.getSaleValue();
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Instant startsAt = product.getSaleStartsAt();
        Instant endsAt = product.getSaleEndsAt();
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (endsAt != null && !now.isBefore(endsAt)) return false;
        return true;
    }

    private BigDecimal applyDiscount(BigDecimal base, SaleType type, BigDecimal value) {
        BigDecimal raw = (type == SaleType.PERCENT)
                ? base.multiply(BigDecimal.ONE.subtract(value.movePointLeft(2)))
                : base.subtract(value);
        return raw.setScale(0, RoundingMode.HALF_UP);
    }

    private Integer percentOff(BigDecimal base, BigDecimal effective) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) return null;
        return base.subtract(effective)
                .multiply(HUNDRED)
                .divide(base, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public record EffectivePrice(
            BigDecimal originalPrice,
            BigDecimal effectivePrice,
            boolean onSale,
            Integer discountPercent
    ) {}
}
