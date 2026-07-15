package com.conceptarena.core.shared.valueobject;

public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
    }

    public static PasswordHash fromPlain(String plain) {
        return new PasswordHash(plain);
    }

    public static PasswordHash fromHash(String hash) {
        return new PasswordHash(hash);
    }
}
