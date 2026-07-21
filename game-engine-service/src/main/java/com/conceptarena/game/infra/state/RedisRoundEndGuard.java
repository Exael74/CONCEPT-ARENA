package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.RoundEndGuard;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed RoundEndGuard — audit gap #7 remediation. SET key value NX is atomic on a single
 * Redis instance, so exactly one of any number of concurrent callers (across any number of
 * game-engine-service replicas) ever wins tryClaim for a given roundId, closing the gap the
 * in-memory version had (each replica's local ConcurrentHashMap could independently "win" its
 * own claim). The TTL is a safety net so a claim isn't held forever if release() is never called
 * (e.g. the claiming replica crashes mid-processing).
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "redis")
public class RedisRoundEndGuard implements RoundEndGuard {

    private static final String KEY_PREFIX = "game:round-end-claim:";
    private static final Duration CLAIM_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public RedisRoundEndGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryClaim(String roundId) {
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + roundId, "claimed", CLAIM_TTL);
        return Boolean.TRUE.equals(claimed);
    }

    @Override
    public void release(String roundId) {
        redisTemplate.delete(KEY_PREFIX + roundId);
    }
}
