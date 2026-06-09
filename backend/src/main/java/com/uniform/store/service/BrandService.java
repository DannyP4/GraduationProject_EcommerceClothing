package com.uniform.store.service;

import com.uniform.store.dto.response.BrandDto;
import com.uniform.store.dto.response.BrandSummaryDto;

import java.util.List;

public interface BrandService {

    List<BrandDto> listBrands(String locale);

    BrandSummaryDto getBrandSummary(Long id, String locale);
}
