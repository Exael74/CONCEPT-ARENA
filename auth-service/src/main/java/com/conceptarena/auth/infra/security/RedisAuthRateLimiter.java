package com.conceptarena.auth.infra.security;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis sliding-window {@link AuthRateLimiter} (audit gap #2): the 10/min-per-IP cap is enforced
 * against a shared Redis sorted set, so it holds across every auth-service replica behind a load
 * balancer — closing the credential-stuffing/DoS window the per-JVM limiter left open. The whole
 * check-and-admit is ONE atomic Lua script (ZREMRANGEBYSCORE drops entries older than the window,
 * ZCARD counts, ZADD+PEXPIRE admits). Active only when app.rate-limiter.store=redis (docker profile).
 */
@Component
@ConditionalOnProperty(name = "app.rate-limiter.store", havingValue = "redis")
public class RedisAuthRateLimiter implements AuthRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000;
    private static final String KEY_PREFIX = "auth:ratelimit:";

    // ARGV: [1]=now(ms) [2]=window(ms) [3]=limit [4]=unique member suffix.
    private static final RedisScript<Long> SLIDING_WINDOW = new DefaultRedisScript<>(
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[1]) - tonumber(ARGV[2])) "
        + "if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[3]) then "
        + "  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. '-' .. ARGV[4]) "
        + "  redis.call('PEXPIRE', KEYS[1], ARGV[2]) "
        + "  return 1 "
        + "else return 0 end",
        Long.class);

    private final StringRedisTemplate redis;

    public RedisAuthRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean allow(String clientKey) {
        long now = System.currentTimeMillis();
        Long allowed = redis.execute(SLIDING_WINDOW, List.of(KEY_PREFIX + clientKey),
            Long.toString(now), Long.toString(WINDOW_MS), Integer.toString(MAX_REQUESTS), UUID.randomUUID().toString());
        return allowed != null && allowed == 1L;
    }
}
