package com.uniform.store.service;

import com.uniform.store.dto.response.DistrictDto;
import com.uniform.store.dto.response.ProvinceDto;
import com.uniform.store.dto.response.WardDto;
import com.uniform.store.enums.ShippingRegion;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingService {

    BigDecimal fee(ShippingRegion region, BigDecimal subtotal);

    BigDecimal freeThreshold();

    List<ProvinceDto> provinces();

    List<DistrictDto> districts(int provinceId);

    List<WardDto> wards(int districtId);

    BigDecimal ghnFee(Integer toDistrictId, String toWardCode, int totalQuantity, BigDecimal subtotal);
}
