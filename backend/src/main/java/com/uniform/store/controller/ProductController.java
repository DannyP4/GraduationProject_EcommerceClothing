package com.uniform.store.controller;

import com.uniform.store.dto.request.ProductFilterRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.dto.response.ProductDetailDto;
import com.uniform.store.dto.response.ProductSummaryDto;
import com.uniform.store.i18n.RequestLocaleResolver;
import com.uniform.store.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;
    private final RequestLocaleResolver localeResolver;

    @GetMapping
    @Operation(summary = "List products")
    public ApiResponse<PageResponse<ProductSummaryDto>> list(
            @ModelAttribute ProductFilterRequest filter,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(productService.listProducts(filter, pageable, locale));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Get product by id or slug")
    public ApiResponse<ProductDetailDto> detail(
            @PathVariable("idOrSlug") String idOrSlug,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(productService.getProduct(idOrSlug, locale));
    }
}
