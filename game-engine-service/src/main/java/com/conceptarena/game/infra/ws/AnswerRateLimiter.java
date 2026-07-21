package com.conceptarena.game.infra.ws;

/**
 * Per-user rate limiter for "answer" submissions (3 per second), enforced on BOTH the WS
 * (GameWebSocketHandler) and REST (GameController) paths so neither transport bypasses anti-cheat.
 *
 * Audit gap #2: two implementations, same profile split as GameStateStore/RoundEndGuard.
 * {@link InMemoryAnswerRateLimiter} (default) counts per JVM — with N replicas the effective limit
 * becomes 3xN/s. {@link RedisAnswerRateLimiter} (app.rate-limiter.store=redis, docker profile) uses
 * a shared Redis sliding window, so the 3/s limit holds across all replicas.
 */
public interface AnswerRateLimiter {

    /** Returns true if this message is allowed under the current window, false if it must be dropped. */
    boolean allow(String userId);
}
