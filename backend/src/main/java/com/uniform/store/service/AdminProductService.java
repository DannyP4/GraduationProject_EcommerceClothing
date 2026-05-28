package com.uniform.store.service;

import com.uniform.store.dto.request.AdminProductFilterRequest;
import com.uniform.store.dto.request.CreateProductRequest;
import com.uniform.store.dto.request.UpdateProductRequest;
import com.uniform.store.dto.response.AdminProductDetailDto;
import com.uniform.store.dto.response.AdminProductSummaryDto;
import com.uniform.store.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminProductService {

    PageResponse<AdminProductSummaryDto> list(AdminProductFilterRequest filter, Pageable pageable);

    AdminProductDetailDto get(Long id);

    AdminProductDetailDto create(CreateProductRequest req);

    AdminProductDetailDto update(Long id, UpdateProductRequest req);

    void softDelete(Long id);

    AdminProductDetailDto restore(Long id);

    void hardDelete(Long id);
}
