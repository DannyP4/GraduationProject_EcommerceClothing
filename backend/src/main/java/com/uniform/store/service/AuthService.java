package com.uniform.store.service;

import com.uniform.store.dto.request.AcceptInviteRequest;
import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import com.uniform.store.dto.request.ResetPasswordRequest;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.dto.response.InvitePreviewResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refreshToken(String refreshToken);
    AuthResponse.UserInfo getCurrentUser(String email);
    void logout(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest req);
    void verifyEmail(String token);
    void resendVerification(String email);
    InvitePreviewResponse previewInvite(String token);
    AuthResponse acceptInvite(AcceptInviteRequest req);
}
