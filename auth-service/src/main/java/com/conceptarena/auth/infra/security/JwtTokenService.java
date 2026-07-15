package com.conceptarena.auth.infra.security;

import com.conceptarena.auth.app.TokenService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Infra implementation of TokenService — delegates to JwtTokenProvider.
 */
@Service
public class JwtTokenService implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String generateToken(String userId) {
        return jwtTokenProvider.generateToken(userId);
    }

    @Override
    public Optional<String> validateAndGetUserId(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return jwtTokenProvider.getUserId(token);
    }
}
