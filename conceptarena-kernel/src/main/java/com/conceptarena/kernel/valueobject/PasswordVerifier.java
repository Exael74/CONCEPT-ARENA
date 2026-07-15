package com.conceptarena.kernel.valueobject;

public interface PasswordVerifier {
    boolean matches(String raw, String hashed);
}
