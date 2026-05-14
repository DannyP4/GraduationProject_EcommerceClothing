package com.uniform.store.service.impl;

import com.uniform.store.dto.fx.FxQuote;
import com.uniform.store.service.FxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class FxServiceImpl implements FxService {

    private final BigDecimal vndUsdRate;
    private final String provider;

    public FxServiceImpl(
            @Value("${app.fx.vnd-usd-rate}") BigDecimal vndUsdRate,
            @Value("${app.fx.provider}") String provider) {
        this.vndUsdRate = vndUsdRate;
        this.provider = provider;
    }

    @Override
    public FxQuote quoteVndToUsd(BigDecimal vndAmount) {
        if (vndAmount == null || vndAmount.signum() <= 0) {
            throw new IllegalArgumentException("vndAmount must be positive");
        }
        BigDecimal usd = vndAmount.multiply(vndUsdRate).setScale(2, RoundingMode.HALF_UP);
        return new FxQuote(
                vndAmount,
                "VND",
                usd,
                "USD",
                vndUsdRate,
                provider,
                Instant.now());
    }
}
