package com.uniform.store.service;

import com.uniform.store.enums.ShippingRegion;

import java.math.BigDecimal;

public interface ShippingService {

    BigDecimal fee(ShippingRegion region, BigDecimal subtotal);

    BigDecimal freeThreshold();
}
