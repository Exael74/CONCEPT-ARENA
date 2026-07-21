package com.conceptarena.room.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link RoomActionRateLimiter}: fixed-window counter in a per-JVM map. Correct for a single
 * instance; see the interface for the multi-replica caveat. Active unless app.rate-limiter.store=redis.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limiter.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRoomActionRateLimiter implements RoomActionRateLimiter {

    private static final int MAX_ACTIONS_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofSeconds(1);

    private final Map<String, Window> windowsByUser = new ConcurrentHashMap<>();

    private static class Window {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant windowStart = Instant.now();
    }

    @Override
    public boolean allow(String userId) {
        Window window = windowsByUser.computeIfAbsent(userId, k -> new Window());
        synchronized (window) {
            if (Duration.between(window.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                window.windowStart = Instant.now();
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= MAX_ACTIONS_PER_WINDOW;
        }
    }
}
