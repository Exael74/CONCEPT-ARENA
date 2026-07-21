package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.RoundEndGuard;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default RoundEndGuard: a local claimed-set, exactly the behavior this class had before it
 * became a port (audit gap #7). Only guarantees one winner within this single JVM — active
 * unless app.game-state.store=redis (see application-docker.yml).
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRoundEndGuard implements RoundEndGuard {

    private final Set<String> claimedRoundIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryClaim(String roundId) {
        return claimedRoundIds.add(roundId);
    }

    @Override
    public void release(String roundId) {
        claimedRoundIds.remove(roundId);
    }
}
