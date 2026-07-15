package com.conceptarena.kernel.error;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
