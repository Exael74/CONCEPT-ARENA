package com.conceptarena.game.app;

/**
 * Port for GameSaga's session state (GameState, the active-round-per-room mapping, and the
 * set of userIds that have answered the active round). Audit gap #7 remediation: previously
 * these lived in plain ConcurrentHashMaps on the GameSaga singleton bean, which is why
 * game-engine-service was single-instance-only (see the removed javadoc on GameSaga). Two
 * implementations: InMemoryGameStateStore (default — every existing test uses this, unchanged
 * single-instance behavior) and RedisGameStateStore (docker/production profile, externalized so
 * 2+ replicas share the same state) — see infra/state/.
 */
public interface GameStateStore {

    /** Returns the existing GameState for roomId, creating and persisting a fresh one if absent. */
    GameState loadOrCreate(String roomId);

    /** Returns the existing GameState for roomId, or null if no game is active for that room. */
    GameState find(String roomId);

    /** Persists (all of) the given state. */
    void save(GameState state);

    /** Removes all state for roomId (game ended or room emptied). */
    void remove(String roomId);

    /** Returns the roundId currently active for roomId, or null if none. */
    String getActiveRound(String roomId);

    void setActiveRound(String roomId, String roundId);

    void clearActiveRound(String roomId);

    /** Records that userId has answered roundId. Returns true if this was a new answer. */
    boolean addAnswered(String roundId, String userId);

    /** Returns how many distinct users have answered roundId so far. */
    int answeredCount(String roundId);

    void clearAnswered(String roundId);
}
