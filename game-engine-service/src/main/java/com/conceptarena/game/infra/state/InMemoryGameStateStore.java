package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.GameState;
import com.conceptarena.game.app.GameStateStore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default GameStateStore: plain in-memory maps, exactly the behavior GameSaga had before audit
 * gap #7's remediation. Single-instance only (see GameStateStore's javadoc) — active unless
 * app.game-state.store=redis (see application-docker.yml).
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryGameStateStore implements GameStateStore {

    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final Map<String, String> activeRoundByRoom = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> answeredByRound = new ConcurrentHashMap<>();

    @Override
    public GameState loadOrCreate(String roomId) {
        return games.computeIfAbsent(roomId, GameState::new);
    }

    @Override
    public GameState find(String roomId) {
        return games.get(roomId);
    }

    @Override
    public void save(GameState state) {
        games.put(state.getRoomId(), state);
    }

    @Override
    public void remove(String roomId) {
        games.remove(roomId);
    }

    @Override
    public String getActiveRound(String roomId) {
        return activeRoundByRoom.get(roomId);
    }

    @Override
    public void setActiveRound(String roomId, String roundId) {
        activeRoundByRoom.put(roomId, roundId);
    }

    @Override
    public void clearActiveRound(String roomId) {
        activeRoundByRoom.remove(roomId);
    }

    @Override
    public boolean addAnswered(String roundId, String userId) {
        return answeredByRound.computeIfAbsent(roundId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    @Override
    public int answeredCount(String roundId) {
        Set<String> answered = answeredByRound.get(roundId);
        return answered == null ? 0 : answered.size();
    }

    @Override
    public void clearAnswered(String roundId) {
        answeredByRound.remove(roundId);
    }
}
