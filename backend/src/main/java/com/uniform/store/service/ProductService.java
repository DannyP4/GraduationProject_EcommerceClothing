package com.uniform.store.service;

import com.uniform.store.dto.request.ProductFilterRequest;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ProductDetailDto;
import com.uniform.store.dto.response.ProductSummaryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    PageResponse<ProductSummaryDto> listProducts(ProductFilterRequest filter, Pageable pageable, String locale);

    ProductDetailDto getProduct(String idOrSlug, String locale);

    List<ProductSummaryDto> getSummariesByIds(List<Long> ids, String locale);
}
