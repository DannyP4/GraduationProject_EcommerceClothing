package com.uniform.store.service;

import com.uniform.store.dto.fx.FxQuote;

import java.math.BigDecimal;

public interface FxService {

    FxQuote quoteVndToUsd(BigDecimal vndAmount);
}
