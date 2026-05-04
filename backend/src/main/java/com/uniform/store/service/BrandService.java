package com.uniform.store.service;

import com.uniform.store.dto.response.BrandDto;

import java.util.List;

public interface BrandService {

    List<BrandDto> listBrands(String locale);
}
