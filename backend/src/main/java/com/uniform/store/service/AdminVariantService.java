package com.uniform.store.service;

import com.uniform.store.dto.request.CreateVariantRequest;
import com.uniform.store.dto.request.UpdateVariantRequest;
import com.uniform.store.dto.response.AdminVariantDto;

import java.util.List;

public interface AdminVariantService {

    List<AdminVariantDto> listByProduct(Long productId);

    AdminVariantDto create(Long productId, CreateVariantRequest req);

    AdminVariantDto update(Long variantId, UpdateVariantRequest req);

    void delete(Long variantId);
}
