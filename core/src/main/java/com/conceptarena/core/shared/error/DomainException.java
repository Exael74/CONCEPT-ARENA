package com.conceptarena.core.shared.error;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
