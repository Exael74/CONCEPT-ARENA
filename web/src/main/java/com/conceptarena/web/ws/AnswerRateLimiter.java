package com.conceptarena.web.ws;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter for "answer" messages on the raw game WebSocket, keyed by the
 * authenticated userId. Unlike RateLimitingFilter (HTTP-only, /api/auth/* endpoints), this
 * guards the actual anti-cheat attack surface: a WS client can otherwise send unlimited
 * SubmitAnswerCommand dispatches with no throttling at all.
 */
@Component
public class AnswerRateLimiter {

    private static final int MAX_MESSAGES_PER_WINDOW = 3;
    private static final Duration WINDOW = Duration.ofSeconds(1);

    private final Map<String, Window> windowsByUser = new ConcurrentHashMap<>();

    private static class Window {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant windowStart = Instant.now();
    }

    /** Returns true if this message is allowed under the current window, false if it must be dropped. */
    public boolean allow(String userId) {
        Window window = windowsByUser.computeIfAbsent(userId, k -> new Window());
        synchronized (window) {
            if (Duration.between(window.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                window.windowStart = Instant.now();
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= MAX_MESSAGES_PER_WINDOW;
        }
    }
}
