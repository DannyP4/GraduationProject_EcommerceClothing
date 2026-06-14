package com.uniform.store.exception;

public class TryOnException extends RuntimeException {

    public TryOnException(String message) {
        super(message);
    }

    public TryOnException(String message, Throwable cause) {
        super(message, cause);
    }
}
