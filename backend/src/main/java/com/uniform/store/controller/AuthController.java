package com.uniform.store.controller;

import com.uniform.store.dto.request.AcceptInviteRequest;
import com.uniform.store.dto.request.ForgotPasswordRequest;
import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import com.uniform.store.dto.request.ResetPasswordRequest;
import com.uniform.store.dto.request.VerifyEmailRequest;
import com.uniform.store.dto.response.ApiResponse;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.dto.response.InvitePreviewResponse;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.AuthService;
import com.uniform.store.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", security = {})
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        captchaService.verify(req.getCaptchaToken(), clientIp(http));
        return ApiResponse.ok("Registered successfully", authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", security = {})
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        captchaService.verify(req.getCaptchaToken(), clientIp(http));
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", security = {})
    public ApiResponse<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("refreshToken is required");
        }
        return ApiResponse.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AuthResponse.UserInfo> me(Authentication authentication) {
        return ApiResponse.ok(authService.getCurrentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (no-op for stateless JWT — FE clears tokens; placeholder for future revocation)")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ApiResponse.ok("Logged out", null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password-reset link", security = {})
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        captchaService.verify(req.getCaptchaToken(), clientIp(http));
        authService.forgotPassword(req.getEmail());
        return ApiResponse.ok("If that email exists, a reset link has been sent.", null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a reset token", security = {})
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ApiResponse.ok("Password updated. You can now sign in.", null);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email using a verification token", security = {})
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.getToken());
        return ApiResponse.ok("Email verified.", null);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email-verification link to the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> resendVerification(Authentication authentication) {
        authService.resendVerification(authentication.getName());
        return ApiResponse.ok("Verification email sent.", null);
    }

    @GetMapping("/invite")
    @Operation(summary = "Preview an admin invitation (does not consume the token)", security = {})
    public ApiResponse<InvitePreviewResponse> previewInvite(@RequestParam String token) {
        return ApiResponse.ok(authService.previewInvite(token));
    }

    @PostMapping("/accept-invite")
    @Operation(summary = "Accept an admin invitation: set a password and sign in", security = {})
    public ApiResponse<AuthResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest req) {
        return ApiResponse.ok("Welcome to the team.", authService.acceptInvite(req));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
