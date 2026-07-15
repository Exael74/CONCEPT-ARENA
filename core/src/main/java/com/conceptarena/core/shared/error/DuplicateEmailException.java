package com.conceptarena.core.shared.error;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
