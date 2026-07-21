package com.conceptarena.game.infra.ws;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * A4: limits new WebSocket handshakes per client IP (fixed window), so a client can't open an
 * unbounded number of connections and exhaust server resources. Complements the per-user message
 * rate limiter (AnswerRateLimiter), which only caps message rate on an already-open connection.
 *
 * Rejecting in beforeHandshake (returning false + 429) blocks the upgrade before a session exists.
 * Per-JVM counting is sufficient here: it bounds connections against THIS instance's resources, which
 * is exactly what we're protecting; a global cap isn't needed to prevent local exhaustion.
 */
@Component
public class WsConnectionRateLimitInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsConnectionRateLimitInterceptor.class);
    private static final int MAX_HANDSHAKES_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();

    private static class Window {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant windowStart = Instant.now();
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ip = clientIp(request);
        Window window = windowsByIp.computeIfAbsent(ip, k -> new Window());
        boolean allowed;
        synchronized (window) {
            if (Duration.between(window.windowStart, Instant.now()).compareTo(WINDOW) > 0) {
                window.windowStart = Instant.now();
                window.count.set(0);
            }
            allowed = window.count.incrementAndGet() <= MAX_HANDSHAKES_PER_WINDOW;
        }
        if (!allowed) {
            log.warn("Rejecting WS handshake from {}: more than {} connections in {}", ip, MAX_HANDSHAKES_PER_WINDOW, WINDOW);
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        }
        return allowed;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String clientIp(ServerHttpRequest request) {
        var forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
