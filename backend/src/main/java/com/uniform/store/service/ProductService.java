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

    List<ProductSummaryDto> getTrendingSummaries(int limit, String locale);

    List<ProductSummaryDto> getSimilarProducts(Long productId, int limit, String locale);

    List<ProductSummaryDto> getFrequentlyBoughtTogether(Long productId, int limit, String locale);

    // Aggregate similar across a set of seed products
    List<ProductSummaryDto> getSimilarToProducts(List<Long> seedIds, int limit, String locale);
}
