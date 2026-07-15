package com.conceptarena.app.user;

import java.util.Optional;

/**
 * Port for generating and validating authentication tokens.
 * App layer declares the interface; infra provides the JWT implementation.
 */
public interface TokenService {
    String generateToken(String userId);

    /** Returns the userId encoded in the token if it is valid (signature + not expired), empty otherwise. */
    Optional<String> validateAndGetUserId(String token);
}
