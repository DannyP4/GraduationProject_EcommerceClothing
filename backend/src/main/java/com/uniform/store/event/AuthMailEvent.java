package com.uniform.store.event;

import com.uniform.store.enums.TokenType;

public record AuthMailEvent(TokenType type, String recipient, String recipientName, String link) {}
