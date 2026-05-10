package com.uniform.store.controller;

import com.uniform.store.dto.request.ChangePasswordRequest;
import com.uniform.store.dto.request.UpdateProfileRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @PatchMapping("/profile")
    @Operation(summary = "Update current user's profile (fullName, phone, preferredLocale)")
    public ApiResponse<AuthResponse.UserInfo> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok("Profile updated",
                profileService.updateProfile(authentication.getName(), req));
    }

    @PostMapping("/password")
    @Operation(summary = "Change current user's password (requires current password)")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest req) {
        profileService.changePassword(authentication.getName(), req);
        return ApiResponse.ok("Password changed", null);
    }
}
