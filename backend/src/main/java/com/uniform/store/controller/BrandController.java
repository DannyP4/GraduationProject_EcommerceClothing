package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.BrandDto;
import com.uniform.store.dto.response.BrandSummaryDto;
import com.uniform.store.i18n.RequestLocaleResolver;
import com.uniform.store.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Tag(name = "Brands")
public class BrandController {

    private final BrandService brandService;
    private final RequestLocaleResolver localeResolver;

    @GetMapping
    @Operation(summary = "List brands")
    public ApiResponse<List<BrandDto>> list(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(brandService.listBrands(locale));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Brand summary with aggregate stats")
    public ApiResponse<BrandSummaryDto> getById(
            @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(brandService.getBrandSummary(id, locale));
    }
}
