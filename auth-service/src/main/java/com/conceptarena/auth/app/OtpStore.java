package com.conceptarena.auth.app;

import java.time.Duration;

/**
 * Stores a short-lived one-time code per email for passwordless OTP login. Two implementations
 * (same profile split as the rate limiters): InMemoryOtpStore (default — dev/tests, no Redis) and
 * RedisOtpStore (docker profile, shared across replicas). The code is single-use and bounded by a
 * TTL and a max-attempts counter to resist brute force.
 */
public interface OtpStore {

    /** How long a generated code stays valid. */
    Duration TTL = Duration.ofMinutes(5);

    /** Wrong verify attempts allowed before the code is invalidated. */
    int MAX_ATTEMPTS = 5;

    /** Stores (overwriting any previous) the code for this email, valid for {@link #TTL}, attempts reset. */
    void store(String email, String code);

    /**
     * Checks a submitted code and, on success, consumes (deletes) it so it can't be reused.
     * On a wrong code it counts the attempt and invalidates the code once MAX_ATTEMPTS is reached.
     */
    VerifyResult verifyAndConsume(String email, String code);

    enum VerifyResult {
        VALID,
        INVALID,
        EXPIRED,            // no active code for this email (never requested, or already expired/used)
        TOO_MANY_ATTEMPTS   // code invalidated after too many wrong tries
    }
}
