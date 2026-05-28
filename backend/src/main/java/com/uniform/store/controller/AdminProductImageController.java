package com.uniform.store.controller;

import com.uniform.store.dto.request.CreateProductImageRequest;
import com.uniform.store.dto.request.UpdateProductImageRequest;
import com.uniform.store.dto.response.AdminProductImageDto;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.service.AdminProductImageService;
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
@Tag(name = "Admin Product Images")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductImageController {

    private final AdminProductImageService adminProductImageService;

    @GetMapping("/products/{productId}/images")
    @Operation(summary = "List images for a product")
    public ApiResponse<List<AdminProductImageDto>> list(@PathVariable Long productId) {
        return ApiResponse.ok(adminProductImageService.listByProduct(productId));
    }

    @PostMapping("/products/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Persist an uploaded image (FE uploads to Cloudinary first, then POSTs url+publicId here)")
    public ApiResponse<AdminProductImageDto> create(@PathVariable Long productId,
                                                    @Valid @RequestBody CreateProductImageRequest req) {
        return ApiResponse.ok("Image added", adminProductImageService.create(productId, req));
    }

    @PutMapping("/images/{id}")
    @Operation(summary = "Update image metadata (altText, sort_order, isPrimary, variantId)")
    public ApiResponse<AdminProductImageDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateProductImageRequest req) {
        return ApiResponse.ok("Image updated", adminProductImageService.update(id, req));
    }

    @DeleteMapping("/images/{id}")
    @Operation(summary = "Delete an image and best-effort remove the Cloudinary asset")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminProductImageService.delete(id);
        return ApiResponse.ok("Image deleted", null);
    }
}
