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
 * own claim).
 *
 * The claim is never explicitly deleted — a round, once ended, must stay claimed for the rest of
 * its life so a late timer can't re-end it (see RoundEndGuard; the old release() was removed
 * 2026-07-21 after it caused exactly that duplicate-end bug). The TTL is the sole memory bound:
 * 10 minutes far exceeds any round's ~30s lifetime and any full 5-round game (~2.5 min), so a
 * round is never re-claimable while its game is still running, and the key is reclaimed long after.
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
}
