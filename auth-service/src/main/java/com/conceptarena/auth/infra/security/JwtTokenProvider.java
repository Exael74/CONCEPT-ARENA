package com.conceptarena.auth.infra.security;

import com.conceptarena.jwtlib.JwtValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * auth-service is the only service that issues tokens, so it's the only one that needs
 * generateToken on top of the shared {@link JwtValidator} (which every other service also
 * depends on, for validate-only). See ADR-005/ADR-007.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final JwtValidator jwtValidator;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:conceptarena-dev-secret-key-must-be-at-least-256-bits-long-for-hs256}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            JwtValidator jwtValidator) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.jwtValidator = jwtValidator;
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
            .subject(userId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    public Optional<String> getUserId(String token) {
        return jwtValidator.validate(token);
    }

    public boolean validateToken(String token) {
        return jwtValidator.validate(token).isPresent();
    }
}
