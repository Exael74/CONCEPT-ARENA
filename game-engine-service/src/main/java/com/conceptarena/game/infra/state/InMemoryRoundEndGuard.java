package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.RoundEndGuard;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default RoundEndGuard: a local claimed-set, only guaranteeing one winner within this single JVM
 * (audit gap #7) — active unless app.game-state.store=redis (see application-docker.yml).
 *
 * A claim is never explicitly released (a round, once ended, must stay claimed so a late timer can't
 * re-end it — see RoundEndGuard). Memory is bounded by a TTL that far exceeds any round's ~30s
 * lifetime: on each claim, entries older than the TTL (long-finished rounds) are purged. This
 * replaces the old release() call as the memory bound; production uses RedisRoundEndGuard's own TTL.
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRoundEndGuard implements RoundEndGuard {

    private static final long TTL_MS = Duration.ofMinutes(10).toMillis();

    private final Map<String, Long> claimedAt = new ConcurrentHashMap<>();

    @Override
    public boolean tryClaim(String roundId) {
        long now = System.currentTimeMillis();
        claimedAt.values().removeIf(claimedTime -> now - claimedTime > TTL_MS);
        return claimedAt.putIfAbsent(roundId, now) == null;
    }
}
