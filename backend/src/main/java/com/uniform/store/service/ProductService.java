package com.uniform.store.service;

import com.uniform.store.dto.response.ProductDetailResponse;
import com.uniform.store.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    Page<ProductSummaryResponse> getProducts(String category, String keyword, String sort, int page, int size);
    ProductDetailResponse getProductBySlug(String slug);
    List<ProductSummaryResponse> getRelatedProducts(String slug);
}
