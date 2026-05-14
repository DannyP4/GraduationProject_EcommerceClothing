package com.uniform.store.dto.fx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// foreign exchange rate
public record FxQuote(
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal convertedAmount,
        String convertedCurrency,
        BigDecimal rate,
        String provider,
        Instant lockedAt) {

    public long convertedAmountInMinorUnits() {
        return convertedAmount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    public Map<String, Object> toSnapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("originalAmount", originalAmount.toPlainString());
        m.put("originalCurrency", originalCurrency);
        m.put("convertedAmount", convertedAmount.toPlainString());
        m.put("convertedCurrency", convertedCurrency);
        m.put("fxRate", rate.toPlainString());
        m.put("fxProvider", provider);
        m.put("fxLockedAt", lockedAt.toString());
        return m;
    }
}
