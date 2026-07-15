package com.conceptarena.core.shared.valueobject;

public interface PasswordVerifier {
    boolean matches(String raw, String hashed);
}
