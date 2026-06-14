package com.uniform.store.controller;

import com.uniform.store.dto.request.TryOnCreateRequest;
import com.uniform.store.dto.request.UploadSignatureRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CloudinarySignatureDto;
import com.uniform.store.dto.response.TryOnJobDto;
import com.uniform.store.service.CloudinaryService;
import com.uniform.store.service.VirtualTryOnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/try-on")
@RequiredArgsConstructor
@Tag(name = "Virtual Try-On")
@SecurityRequirement(name = "bearerAuth")
public class TryOnController {

    private static final String TRYON_UPLOAD_FOLDER = "uniform/tryon";

    private final VirtualTryOnService tryOnService;
    private final CloudinaryService cloudinaryService;

    @PostMapping
    @Operation(summary = "Start a virtual try-on render for a catalog product")
    public ApiResponse<TryOnJobDto> create(Authentication auth, @Valid @RequestBody TryOnCreateRequest req) {
        return ApiResponse.ok(tryOnService.createJob(auth.getName(), req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Poll the status/result of a try-on render")
    public ApiResponse<TryOnJobDto> get(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(tryOnService.getJob(auth.getName(), id));
    }

    @PostMapping("/upload-signature")
    @Operation(summary = "Cloudinary signed-upload payload for the user's try-on photo")
    public ApiResponse<CloudinarySignatureDto> sign(@Valid @RequestBody(required = false) UploadSignatureRequest req) {
        String hint = req == null ? null : req.getFilenameHint();
        return ApiResponse.ok(cloudinaryService.generateSignedUploadParams(TRYON_UPLOAD_FOLDER, hint));
    }
}
