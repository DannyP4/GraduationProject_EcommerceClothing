package com.uniform.store.service;

import com.uniform.store.dto.request.ChangePasswordRequest;
import com.uniform.store.dto.request.UpdateProfileRequest;
import com.uniform.store.dto.response.AuthResponse;

public interface ProfileService {

    AuthResponse.UserInfo updateProfile(String email, UpdateProfileRequest req);

    void changePassword(String email, ChangePasswordRequest req);
}
