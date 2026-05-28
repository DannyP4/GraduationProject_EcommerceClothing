package com.uniform.store.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountInactiveException extends AuthenticationException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
