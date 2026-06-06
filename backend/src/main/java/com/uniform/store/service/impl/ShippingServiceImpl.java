package com.uniform.store.service.impl;

import com.uniform.store.config.ShippingProperties;
import com.uniform.store.enums.ShippingRegion;
import com.uniform.store.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingProperties props;

    @Override
    public BigDecimal fee(ShippingRegion region, BigDecimal subtotal) {
        if (subtotal != null && props.getFreeThreshold() != null
                && subtotal.compareTo(props.getFreeThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }
        return baseRate(region);
    }

    @Override
    public BigDecimal freeThreshold() {
        return props.getFreeThreshold();
    }

    private BigDecimal baseRate(ShippingRegion region) {
        if (region == null) return props.getSouth();
        return switch (region) {
            case NORTH -> props.getNorth();
            case CENTRAL -> props.getCentral();
            case SOUTH -> props.getSouth();
        };
    }
}
