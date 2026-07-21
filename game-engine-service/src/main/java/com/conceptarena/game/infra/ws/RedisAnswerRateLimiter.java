package com.conceptarena.game.infra.ws;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis sliding-window {@link AnswerRateLimiter} (audit gap #2): the 3/s-per-user limit is enforced
 * against a shared Redis sorted set, so it holds no matter how many game-engine replicas serve the
 * traffic (the in-memory limiter's limit multiplied by the replica count). The whole check-and-admit
 * is ONE atomic Lua script — ZREMRANGEBYSCORE drops entries older than the window, ZCARD counts the
 * rest, ZADD+PEXPIRE admits — so concurrent requests on different replicas can't race past the cap.
 * Active only when app.rate-limiter.store=redis (docker profile).
 */
@Component
@ConditionalOnProperty(name = "app.rate-limiter.store", havingValue = "redis")
public class RedisAnswerRateLimiter implements AnswerRateLimiter {

    private static final int MAX_MESSAGES = 3;
    private static final long WINDOW_MS = 1000;
    private static final String KEY_PREFIX = "game:ratelimit:answer:";

    // ARGV: [1]=now(ms) [2]=window(ms) [3]=limit [4]=unique member suffix (avoids ZADD collisions
    // when two requests share the same millisecond timestamp).
    private static final RedisScript<Long> SLIDING_WINDOW = new DefaultRedisScript<>(
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[1]) - tonumber(ARGV[2])) "
        + "if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[3]) then "
        + "  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. '-' .. ARGV[4]) "
        + "  redis.call('PEXPIRE', KEYS[1], ARGV[2]) "
        + "  return 1 "
        + "else return 0 end",
        Long.class);

    private final StringRedisTemplate redis;

    public RedisAnswerRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean allow(String userId) {
        long now = System.currentTimeMillis();
        Long allowed = redis.execute(SLIDING_WINDOW, List.of(KEY_PREFIX + userId),
            Long.toString(now), Long.toString(WINDOW_MS), Integer.toString(MAX_MESSAGES), UUID.randomUUID().toString());
        return allowed != null && allowed == 1L;
    }
}
