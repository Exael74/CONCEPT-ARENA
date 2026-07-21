package com.conceptarena.game.app;

/**
 * Serializes the load-modify-save sequence GameSaga performs on a room's GameState.
 *
 * Audit gap #4 remediation: RedisGameStateStore's read-modify-write on the GameState JSON blob was
 * not atomic across replicas, so two game-engine-service instances handling events for the SAME
 * room at the same instant could clobber each other's update (last-write-wins on participants /
 * scores / currentRound). GameSaga now runs each of its event handlers inside
 * {@code runExclusively(roomId, ...)}, so all mutations for one room are serialized.
 *
 * The lock MUST be reentrant per thread: the local event bus dispatches synchronously, so
 * onRoundEnded -> StartRoundCommand -> RoundStarted -> onRoundStarted runs on a single thread that
 * already holds the room lock. A non-reentrant lock would self-deadlock there.
 *
 * Two implementations (same profile split as GameStateStore): InMemoryGameStateLock (default, a
 * per-room ReentrantLock — reentrant by nature) and RedisGameStateLock (redis profile, a reentrant
 * distributed lock so the serialization holds across replicas).
 */
public interface GameStateLock {

    /** Runs {@code action} while holding the exclusive lock for {@code roomId}; reentrant per thread. */
    void runExclusively(String roomId, Runnable action);
}
