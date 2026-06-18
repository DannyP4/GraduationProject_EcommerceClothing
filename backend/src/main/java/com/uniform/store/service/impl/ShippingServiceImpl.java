package com.uniform.store.service.impl;

import com.uniform.store.config.GhnProperties;
import com.uniform.store.config.ShippingProperties;
import com.uniform.store.dto.response.DistrictDto;
import com.uniform.store.dto.response.ProvinceDto;
import com.uniform.store.dto.response.WardDto;
import com.uniform.store.enums.ShippingRegion;
import com.uniform.store.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingProperties props;
    private final GhnProperties ghnProps;
    private final GhnClient ghnClient;

    @Override
    public BigDecimal fee(ShippingRegion region, BigDecimal subtotal) {
        if (isFree(subtotal)) {
            return BigDecimal.ZERO;
        }
        return baseRate(region);
    }

    @Override
    public BigDecimal freeThreshold() {
        return props.getFreeThreshold();
    }

    @Override
    public List<ProvinceDto> provinces() {
        return ghnClient.provinces();
    }

    @Override
    public List<DistrictDto> districts(int provinceId) {
        return ghnClient.districts(provinceId);
    }

    @Override
    public List<WardDto> wards(int districtId) {
        return ghnClient.wards(districtId);
    }

    @Override
    public BigDecimal ghnFee(Integer toDistrictId, String toWardCode, int totalQuantity, BigDecimal subtotal) {
        if (isFree(subtotal)) {
            return BigDecimal.ZERO;
        }
        if (toDistrictId != null && toWardCode != null && !toWardCode.isBlank()) {
            int weight = Math.max(totalQuantity, 1) * ghnProps.getDefaultItemWeightGrams();
            return ghnClient.calculateFee(toDistrictId, toWardCode, weight)
                    .orElseGet(() -> baseRate(ShippingRegion.SOUTH));
        }
        return baseRate(ShippingRegion.SOUTH);
    }

    private boolean isFree(BigDecimal subtotal) {
        return subtotal != null && props.getFreeThreshold() != null
                && subtotal.compareTo(props.getFreeThreshold()) >= 0;
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
