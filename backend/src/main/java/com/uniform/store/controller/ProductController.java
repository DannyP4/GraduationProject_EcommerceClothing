package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.ProductDetailResponse;
import com.uniform.store.dto.response.ProductSummaryResponse;
import com.uniform.store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.ok(productService.getProducts(category, keyword, sort, page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable String slug) {
        return ApiResponse.ok(productService.getProductBySlug(slug));
    }

    @GetMapping("/{slug}/related")
    public ApiResponse<List<ProductSummaryResponse>> getRelated(@PathVariable String slug) {
        return ApiResponse.ok(productService.getRelatedProducts(slug));
    }
}
