package com.uniform.store.controller;

import com.uniform.store.dto.request.AdminProductFilterRequest;
import com.uniform.store.dto.request.CreateProductRequest;
import com.uniform.store.dto.request.UpdateProductRequest;
import com.uniform.store.dto.response.AdminProductDetailDto;
import com.uniform.store.dto.response.AdminProductSummaryDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.PageResponse;
import com.uniform.store.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin Products")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    @Operation(summary = "List products with admin-side filters (includes inactive + optionally deleted)")
    public ApiResponse<PageResponse<AdminProductSummaryDto>> list(
            @ModelAttribute AdminProductFilterRequest filter,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(adminProductService.list(filter, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail including variants + images")
    public ApiResponse<AdminProductDetailDto> get(@PathVariable Long id) {
        return ApiResponse.ok(adminProductService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ApiResponse<AdminProductDetailDto> create(@Valid @RequestBody CreateProductRequest req) {
        return ApiResponse.ok("Product created", adminProductService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ApiResponse<AdminProductDetailDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateProductRequest req) {
        return ApiResponse.ok("Product updated", adminProductService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product (sets deleted_at and isActive=false)")
    public ApiResponse<Void> softDelete(@PathVariable Long id) {
        adminProductService.softDelete(id);
        return ApiResponse.ok("Product soft-deleted", null);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted product")
    public ApiResponse<AdminProductDetailDto> restore(@PathVariable Long id) {
        return ApiResponse.ok("Product restored", adminProductService.restore(id));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Permanently delete a soft-deleted product (blocked if variants are referenced by orders)")
    public ApiResponse<Void> hardDelete(@PathVariable Long id) {
        adminProductService.hardDelete(id);
        return ApiResponse.ok("Product permanently deleted", null);
    }
}
