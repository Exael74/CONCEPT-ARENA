package com.conceptarena.auth.app;

import java.util.Optional;

/**
 * Port for generating and validating authentication tokens. Only auth-service needs
 * generateToken (every other service only ever needs the shared jwt-validation-lib's
 * JwtValidator.validate, since only auth-service issues tokens — see ADR-005/ADR-007).
 */
public interface TokenService {
    String generateToken(String userId);

    /** Returns the userId encoded in the token if it is valid (signature + not expired), empty otherwise. */
    Optional<String> validateAndGetUserId(String token);
}
