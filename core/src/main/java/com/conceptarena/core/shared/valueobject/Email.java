package com.conceptarena.core.shared.valueobject;

import com.conceptarena.core.shared.error.DomainException;

public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new DomainException("Invalid email format");
        }
    }
}
