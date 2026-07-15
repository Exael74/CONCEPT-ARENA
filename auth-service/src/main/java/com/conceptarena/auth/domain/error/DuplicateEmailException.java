package com.conceptarena.auth.domain.error;

import com.conceptarena.kernel.error.DomainException;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
