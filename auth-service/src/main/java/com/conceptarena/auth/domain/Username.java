package com.conceptarena.auth.domain;

import com.conceptarena.kernel.error.DomainException;

public record Username(String value) {
    public Username {
        if (value == null || !value.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new DomainException("Username must be 3-20 characters: letters, numbers, underscore only");
        }
    }
}
