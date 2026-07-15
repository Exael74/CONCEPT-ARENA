package com.conceptarena.jwtlib;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies an HS256 JWT's signature and extracts its subject (userId). Every service that needs
 * to authenticate a request or WebSocket handshake without calling auth-service synchronously
 * depends on this class instead of re-implementing signature verification locally (see
 * docs/architecture-decisions/ADR-005-jwt-hs256-shared-secret.md and ADR-007). Only auth-service
 * additionally needs to *issue* tokens — that responsibility stays there, not here.
 */
public class JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    private final SecretKey secretKey;

    public JwtValidator(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the token's subject (userId) if the signature is valid and it isn't expired, else empty. */
    public Optional<String> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
