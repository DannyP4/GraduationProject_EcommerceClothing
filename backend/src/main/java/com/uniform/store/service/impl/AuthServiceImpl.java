package com.uniform.store.service.impl;

import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.exception.AccountInactiveException;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.RoleRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.security.JwtUtil;
import com.uniform.store.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_LOCALE = "en";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email already registered: " + normalizedEmail);
        }

        Role customerRole = roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(
                        "Default '" + Role.CUSTOMER + "' role is missing — check V1 seed data"));

        String locale = (req.getPreferredLocale() != null && !req.getPreferredLocale().isBlank())
                ? req.getPreferredLocale()
                : DEFAULT_LOCALE;

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName().trim())
                .phone(req.getPhone() != null && !req.getPhone().isBlank()
                        ? req.getPhone().trim()
                        : null)
                .preferredLocale(locale)
                .role(customerRole)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountInactiveException("Account " + user.getStatus().name().toLowerCase());
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toUserInfo(user);
    }

    @Override
    public void logout(String email) {
        // Stateless JWT: no server-side state.
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountInactiveException("Account " + user.getStatus().name().toLowerCase());
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirySeconds())
                .user(toUserInfo(user))
                .build();
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
