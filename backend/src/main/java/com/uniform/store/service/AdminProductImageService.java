package com.uniform.store.service;

import com.uniform.store.dto.request.CreateProductImageRequest;
import com.uniform.store.dto.request.UpdateProductImageRequest;
import com.uniform.store.dto.response.AdminProductImageDto;

import java.util.List;

public interface AdminProductImageService {

    List<AdminProductImageDto> listByProduct(Long productId);

    AdminProductImageDto create(Long productId, CreateProductImageRequest req);

    AdminProductImageDto update(Long imageId, UpdateProductImageRequest req);

    void delete(Long imageId);
}
