package com.uniform.store.security;

public record OAuthUserInfo(String email, boolean emailVerified, String subject, String fullName) {
}
