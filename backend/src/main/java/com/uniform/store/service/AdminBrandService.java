package com.uniform.store.service;

import com.uniform.store.dto.request.CreateBrandRequest;
import com.uniform.store.dto.request.UpdateBrandRequest;
import com.uniform.store.dto.response.AdminBrandDto;

import java.util.List;

public interface AdminBrandService {

    List<AdminBrandDto> listAll();

    AdminBrandDto get(Long id);

    AdminBrandDto create(CreateBrandRequest req);

    AdminBrandDto update(Long id, UpdateBrandRequest req);

    void delete(Long id);
}
