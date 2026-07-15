package com.conceptarena.auth.domain.error;

import com.conceptarena.kernel.error.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
