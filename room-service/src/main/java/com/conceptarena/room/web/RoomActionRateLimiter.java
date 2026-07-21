package com.conceptarena.room.web;

/**
 * Per-user rate limiter for room write actions (create / join / join-by-code / leave), 5 per second.
 *
 * Audit gap #2 (and originally gap #5, which added the limiter at all): two implementations, same
 * profile split as the rest of the codebase. {@link InMemoryRoomActionRateLimiter} (default) counts
 * per JVM — with N replicas the effective limit becomes 5xN/s. {@link RedisRoomActionRateLimiter}
 * (app.rate-limiter.store=redis, docker profile) uses a shared Redis sliding window so the 5/s limit
 * holds across replicas. game-engine's AnswerRateLimiter cannot be reused (cross-service classpath
 * coupling breaks the per-service boundary), so this is an intentional independent duplicate.
 */
public interface RoomActionRateLimiter {

    /** Returns true if this action is allowed under the current window, false if it must be rejected. */
    boolean allow(String userId);
}
