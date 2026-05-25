package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateBrandRequest;
import com.uniform.store.dto.request.UpdateBrandRequest;
import com.uniform.store.dto.response.AdminBrandDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.AdminBrandService;
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
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
@Tag(name = "Admin Brands")
@SecurityRequirement(name = "bearerAuth")
public class AdminBrandController {

    private final AdminBrandService adminBrandService;

    @GetMapping
    @Operation(summary = "List all brands")
    public ApiResponse<List<AdminBrandDto>> list() {
        return ApiResponse.ok(adminBrandService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single brand")
    public ApiResponse<AdminBrandDto> get(@PathVariable Long id) {
        return ApiResponse.ok(adminBrandService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a brand")
    public ApiResponse<AdminBrandDto> create(@Valid @RequestBody CreateBrandRequest req) {
        return ApiResponse.ok("Brand created", adminBrandService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a brand")
    public ApiResponse<AdminBrandDto> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateBrandRequest req) {
        return ApiResponse.ok("Brand updated", adminBrandService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brand (blocked if it has products)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminBrandService.delete(id);
        return ApiResponse.ok("Brand deleted", null);
    }
}
