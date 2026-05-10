package com.uniform.store.service.impl;

import com.uniform.store.dto.request.ChangePasswordRequest;
import com.uniform.store.dto.request.UpdateProfileRequest;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.entity.User;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse.UserInfo updateProfile(String email, UpdateProfileRequest req) {
        User user = loadUser(email);

        // PATCH semantics: only set fields the client sent (non-null).
        if (req.getFullName() != null) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getPhone() != null) {
            // Empty string clears the phone; non-empty trims and stores.
            user.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        }
        if (req.getPreferredLocale() != null) {
            user.setPreferredLocale(req.getPreferredLocale());
        }

        user = userRepository.save(user);
        return toUserInfo(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = loadUser(email);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new BadRequestException("New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        // Existing access/refresh tokens stay valid
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .preferredLocale(user.getPreferredLocale())
                .build();
    }
}
