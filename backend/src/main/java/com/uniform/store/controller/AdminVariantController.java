package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateVariantRequest;
import com.uniform.store.dto.request.UpdateVariantRequest;
import com.uniform.store.dto.response.AdminVariantDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.AdminVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Variants")
@SecurityRequirement(name = "bearerAuth")
public class AdminVariantController {

    private final AdminVariantService adminVariantService;

    @GetMapping("/products/{productId}/variants")
    @Operation(summary = "List variants for a product")
    public ApiResponse<List<AdminVariantDto>> list(@PathVariable Long productId) {
        return ApiResponse.ok(adminVariantService.listByProduct(productId));
    }

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a variant under a product")
    public ApiResponse<AdminVariantDto> create(@PathVariable Long productId,
                                               @Valid @RequestBody CreateVariantRequest req) {
        return ApiResponse.ok("Variant created", adminVariantService.create(productId, req));
    }

    @PutMapping("/variants/{id}")
    @Operation(summary = "Update a variant")
    public ApiResponse<AdminVariantDto> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateVariantRequest req) {
        return ApiResponse.ok("Variant updated", adminVariantService.update(id, req));
    }

    @DeleteMapping("/variants/{id}")
    @Operation(summary = "Delete a variant (blocked if referenced by orders)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminVariantService.delete(id);
        return ApiResponse.ok("Variant deleted", null);
    }
}
