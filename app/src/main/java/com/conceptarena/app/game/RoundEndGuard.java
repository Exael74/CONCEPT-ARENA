package com.conceptarena.app.game;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Prevents a round from being ended twice. The scheduled timer (ScheduledTimerAdapter)
 * and the early-end path (GameSaga.triggerEarlyRoundEnd) race independently: cancelling
 * a ScheduledFuture does not stop a task that already started running, so both paths can
 * reach the "end this round and publish RoundEnded" step for the same roundId. Only the
 * caller that wins tryClaim actually ends the round; the loser must skip its own end logic.
 */
@Component
public class RoundEndGuard {

    private final Set<String> claimedRoundIds = ConcurrentHashMap.newKeySet();

    /** Returns true if this call is the first to claim ending this round. */
    public boolean tryClaim(String roundId) {
        return claimedRoundIds.add(roundId);
    }

    /** Frees the roundId once it has been fully processed, to bound memory over a long-running process. */
    public void release(String roundId) {
        claimedRoundIds.remove(roundId);
    }
}
