package com.uniform.store.service.impl;

import com.uniform.store.config.AppAuthProperties;
import com.uniform.store.dto.request.AcceptInviteRequest;
import com.uniform.store.dto.request.LoginRequest;
import com.uniform.store.dto.request.RegisterRequest;
import com.uniform.store.dto.request.ResetPasswordRequest;
import com.uniform.store.dto.response.AuthResponse;
import com.uniform.store.dto.response.InvitePreviewResponse;
import com.uniform.store.entity.OneTimeToken;
import com.uniform.store.entity.Role;
import com.uniform.store.entity.User;
import com.uniform.store.enums.AuthProvider;
import com.uniform.store.enums.TokenType;
import com.uniform.store.enums.UserStatus;
import com.uniform.store.security.OAuthUserInfo;
import com.uniform.store.event.AuthMailEvent;
import com.uniform.store.exception.AccountInactiveException;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.exception.ResourceNotFoundException;
import com.uniform.store.repository.RoleRepository;
import com.uniform.store.repository.UserRepository;
import com.uniform.store.security.JwtUtil;
import com.uniform.store.service.AuthService;
import com.uniform.store.service.OneTimeTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_LOCALE = "en";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OneTimeTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppAuthProperties authProps;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

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
        issueVerify(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        String normalizedEmail = req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPasswordHash() == null) {
            throw new BadRequestException(
                    "This account uses Google sign-in. Continue with Google, or set a password via Forgot password.");
        }

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
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String rawToken = tokenService.issue(TokenType.PASSWORD_RESET, user.getEmail(), user.getId(),
                    Duration.ofMinutes(authProps.getResetTokenTtlMinutes()), null);
            String link = frontendBaseUrl + "/auth/reset-password?token=" + rawToken;
            eventPublisher.publishEvent(new AuthMailEvent(
                    TokenType.PASSWORD_RESET, user.getEmail(), user.getFullName(), link));
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        OneTimeToken token = tokenService.consume(req.getToken(), TokenType.PASSWORD_RESET);
        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        OneTimeToken consumed = tokenService.consume(token, TokenType.EMAIL_VERIFY);
        User user = userRepository.findByEmail(consumed.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (user.getEmailVerifiedAt() != null) {
            throw new BadRequestException("Email is already verified");
        }
        issueVerify(user);
    }

    private void issueVerify(User user) {
        String rawToken = tokenService.issue(TokenType.EMAIL_VERIFY, user.getEmail(), user.getId(),
                Duration.ofHours(authProps.getVerifyTokenTtlHours()), null);
        String link = frontendBaseUrl + "/auth/verify-email?token=" + rawToken;
        eventPublisher.publishEvent(new AuthMailEvent(
                TokenType.EMAIL_VERIFY, user.getEmail(), user.getFullName(), link));
    }

    @Override
    @Transactional(readOnly = true)
    public InvitePreviewResponse previewInvite(String token) {
        OneTimeToken invite = tokenService.peek(token, TokenType.ADMIN_INVITE);
        String fullName = invite.getPayload() != null
                ? (String) invite.getPayload().get("fullName")
                : null;
        return InvitePreviewResponse.builder()
                .email(invite.getEmail())
                .fullName(fullName)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest req) {
        OneTimeToken invite = tokenService.consume(req.getToken(), TokenType.ADMIN_INVITE);
        if (userRepository.existsByEmail(invite.getEmail())) {
            throw new BadRequestException("A user with this email already exists");
        }

        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "'" + Role.ADMIN + "' role is missing — check V1 seed data"));

        User user = User.builder()
                .email(invite.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName().trim())
                .preferredLocale(DEFAULT_LOCALE)
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(Instant.now())
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public String startOAuthHandoff(OAuthUserInfo info) {
        if (info.email() == null || info.email().isBlank()) {
            throw new BadRequestException("Google did not return an email");
        }
        String email = info.email().trim().toLowerCase();

        User user = null;
        if (info.subject() != null) {
            user = userRepository.findByOauthSubject(info.subject()).orElse(null);
        }
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            Role customerRole = roleRepository.findByName(Role.CUSTOMER)
                    .orElseThrow(() -> new IllegalStateException(
                            "Default '" + Role.CUSTOMER + "' role is missing — check V1 seed data"));
            user = userRepository.save(User.builder()
                    .email(email)
                    .fullName(info.fullName() != null && !info.fullName().isBlank() ? info.fullName().trim() : email)
                    .preferredLocale(DEFAULT_LOCALE)
                    .role(customerRole)
                    .status(UserStatus.ACTIVE)
                    .emailVerifiedAt(Instant.now())
                    .authProvider(AuthProvider.GOOGLE)
                    .oauthSubject(info.subject())
                    .build());
        } else {
            if (user.getOauthSubject() == null) {
                if (!info.emailVerified()) {
                    throw new BadRequestException("Google has not verified this email");
                }
                user.setOauthSubject(info.subject());
                if (user.getEmailVerifiedAt() == null) {
                    user.setEmailVerifiedAt(Instant.now());
                }
                userRepository.save(user);
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new AccountInactiveException("Account " + user.getStatus().name().toLowerCase());
            }
        }

        return tokenService.issue(TokenType.OAUTH_HANDOFF, user.getEmail(), user.getId(),
                Duration.ofSeconds(authProps.getOauthHandoffTtlSeconds()), null);
    }

    @Override
    @Transactional
    public AuthResponse oauthExchange(String code) {
        OneTimeToken token = tokenService.consume(code, TokenType.OAUTH_HANDOFF);
        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountInactiveException("Account " + user.getStatus().name().toLowerCase());
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return buildAuthResponse(user);
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
                .emailVerified(user.getEmailVerifiedAt() != null)
                .build();
    }
}
