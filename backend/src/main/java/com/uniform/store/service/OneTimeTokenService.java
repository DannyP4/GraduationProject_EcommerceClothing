package com.uniform.store.service;

import com.uniform.store.entity.OneTimeToken;
import com.uniform.store.enums.TokenType;

import java.time.Duration;
import java.util.Map;

public interface OneTimeTokenService {

    String issue(TokenType type, String email, Long userId, Duration ttl, Map<String, Object> payload);

    OneTimeToken consume(String rawToken, TokenType type);

    OneTimeToken peek(String rawToken, TokenType type);
}
