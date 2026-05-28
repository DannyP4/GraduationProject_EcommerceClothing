package com.uniform.store.controller;

import com.uniform.store.dto.request.UploadSignatureRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.CloudinarySignatureDto;
import com.uniform.store.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/upload-signature")
@RequiredArgsConstructor
@Tag(name = "Admin Upload")
@SecurityRequirement(name = "bearerAuth")
public class AdminUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping
    @Operation(summary = "Generate a Cloudinary signed-upload payload for direct browser upload")
    public ApiResponse<CloudinarySignatureDto> sign(@Valid @RequestBody(required = false) UploadSignatureRequest req) {
        String folder = req == null ? null : req.getFolder();
        String hint = req == null ? null : req.getFilenameHint();
        return ApiResponse.ok(cloudinaryService.generateSignedUploadParams(folder, hint));
    }
}
