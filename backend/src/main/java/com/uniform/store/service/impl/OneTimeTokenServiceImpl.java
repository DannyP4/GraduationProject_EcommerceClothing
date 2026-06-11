package com.uniform.store.service.impl;

import com.uniform.store.entity.OneTimeToken;
import com.uniform.store.enums.TokenType;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.repository.OneTimeTokenRepository;
import com.uniform.store.service.OneTimeTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OneTimeTokenServiceImpl implements OneTimeTokenService {

    private static final int TOKEN_BYTES = 32;

    private final OneTimeTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String issue(TokenType type, String email, Long userId, Duration ttl, Map<String, Object> payload) {
        String rawToken = generateToken();
        tokenRepository.save(OneTimeToken.builder()
                .type(type)
                .email(email)
                .userId(userId)
                .tokenHash(sha256(rawToken))
                .payload(payload)
                .expiresAt(Instant.now().plus(ttl))
                .build());
        return rawToken;
    }

    @Override
    @Transactional
    public OneTimeToken consume(String rawToken, TokenType type) {
        OneTimeToken token = findValid(rawToken, type);
        token.setConsumedAt(Instant.now());
        return tokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public OneTimeToken peek(String rawToken, TokenType type) {
        return findValid(rawToken, type);
    }

    private OneTimeToken findValid(String rawToken, TokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invalid or expired token");
        }
        OneTimeToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.getType() == type)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        if (token.getConsumedAt() != null) {
            throw new BadRequestException("This link has already been used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This link has expired");
        }
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
