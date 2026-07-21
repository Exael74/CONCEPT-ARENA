package com.conceptarena.game.app;

/**
 * Prevents a round from being ended twice. The scheduled timer (ScheduledTimerAdapter)
 * and the early-end path (GameSaga.triggerEarlyRoundEnd) race independently: cancelling
 * a ScheduledFuture does not stop a task that already started running, so both paths can
 * reach the "end this round and publish RoundEnded" step for the same roundId. Only the
 * caller that wins tryClaim actually ends the round; the loser must skip its own end logic.
 *
 * Audit gap #7 remediation (2026-07-15): this was a concrete class backed by a local
 * ConcurrentHashMap, which only guarantees "only one winner" within a single JVM — with
 * game-engine-service running 2+ replicas, both the timer path in replica A and the early-end
 * path in replica B could each win their own local claim for the same roundId and both publish
 * RoundEnded. Now a port with two implementations: InMemoryRoundEndGuard (default, single
 * replica — used by every existing test) and RedisRoundEndGuard (docker/production profile,
 * SETNX-based, safe across replicas) — see infra/state/.
 */
public interface RoundEndGuard {

    /** Returns true if this call is the first to claim ending this round. */
    boolean tryClaim(String roundId);

    /** Frees the roundId once it has been fully processed, to bound memory/keys over time. */
    void release(String roundId);
}
