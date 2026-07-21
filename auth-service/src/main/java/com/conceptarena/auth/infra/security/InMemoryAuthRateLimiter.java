package com.conceptarena.auth.infra.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link AuthRateLimiter}: fixed-window counter in a per-JVM map. Correct for a single
 * instance; see the interface for the multi-replica caveat. Active unless app.rate-limiter.store=redis.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limiter.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryAuthRateLimiter implements AuthRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, RequestWindow> windowsByClient = new ConcurrentHashMap<>();

    private static class RequestWindow {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant windowStart = Instant.now();
    }

    @Override
    public boolean allow(String clientKey) {
        RequestWindow window = windowsByClient.computeIfAbsent(clientKey, k -> new RequestWindow());
        synchronized (window) {
            if (Duration.between(window.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                window.windowStart = Instant.now();
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
        }
    }
}
