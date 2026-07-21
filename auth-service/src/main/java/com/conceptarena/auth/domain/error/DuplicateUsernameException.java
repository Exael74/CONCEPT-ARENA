package com.conceptarena.auth.domain.error;

import com.conceptarena.kernel.error.DomainException;

public class DuplicateUsernameException extends DomainException {
    public DuplicateUsernameException(String username) {
        super("Username already taken: " + username);
    }
}
