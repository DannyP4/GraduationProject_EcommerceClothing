package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateCategoryRequest;
import com.uniform.store.dto.request.UpdateCategoryRequest;
import com.uniform.store.dto.response.AdminCategoryDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.AdminCategoryService;
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
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin Categories")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    @Operation(summary = "List categories as a tree")
    public ApiResponse<List<AdminCategoryDto>> list() {
        return ApiResponse.ok(adminCategoryService.listTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category")
    public ApiResponse<AdminCategoryDto> get(@PathVariable Long id) {
        return ApiResponse.ok(adminCategoryService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public ApiResponse<AdminCategoryDto> create(@Valid @RequestBody CreateCategoryRequest req) {
        return ApiResponse.ok("Category created", adminCategoryService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ApiResponse<AdminCategoryDto> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateCategoryRequest req) {
        return ApiResponse.ok("Category updated", adminCategoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (blocked if it has products or children)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return ApiResponse.ok("Category deleted", null);
    }
}
