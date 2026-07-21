package com.conceptarena.auth.infra.security;

/**
 * Rate limiter for the auth endpoints (/api/auth/login, /api/auth/register), 10 requests/minute,
 * keyed by client IP. Used by {@link RateLimitingFilter}.
 *
 * Audit gap #2: two implementations, same profile split as the rest of the codebase.
 * {@link InMemoryAuthRateLimiter} (default) counts per JVM — with N replicas the effective limit
 * becomes 10xN/min, which is a real DoS/credential-stuffing gap. {@link RedisAuthRateLimiter}
 * (app.rate-limiter.store=redis, docker profile) uses a shared Redis sliding window so the 10/min
 * cap holds across replicas.
 */
public interface AuthRateLimiter {

    /** Returns true if this request is allowed under the current window, false if it must be rejected. */
    boolean allow(String clientKey);
}
