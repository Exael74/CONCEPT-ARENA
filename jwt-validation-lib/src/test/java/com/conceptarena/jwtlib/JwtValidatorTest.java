package com.conceptarena.jwtlib;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtValidatorTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-alg";

    private String tokenSignedWith(String secret, String subject, long expiresInMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiresInMs))
            .signWith(key)
            .compact();
    }

    @Test
    void validatesAndExtractsSubjectFromCorrectlySignedToken() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith(SECRET, "user-123", 60_000);

        assertThat(validator.validate(token)).contains("user-123");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith("a-completely-different-secret-key-of-sufficient-length-too", "user-123", 60_000);

        assertThat(validator.validate(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith(SECRET, "user-123", -1_000);

        assertThat(validator.validate(token)).isEmpty();
    }

    @Test
    void rejectsGarbageInput() {
        JwtValidator validator = new JwtValidator(SECRET);

        assertThat(validator.validate("not-a-jwt")).isEmpty();
    }
}
