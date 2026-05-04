package com.uniform.store.controller;

import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CategoryDto;
import com.uniform.store.i18n.RequestLocaleResolver;
import com.uniform.store.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final RequestLocaleResolver localeResolver;

    @GetMapping
    @Operation(summary = "List categories")
    public ApiResponse<List<CategoryDto>> list(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = localeResolver.resolve(acceptLanguage);
        return ApiResponse.ok(categoryService.listCategories(locale));
    }
}
