package com.conceptarena.game.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mutable per-room game-session state GameSaga orchestrates. Moved out of GameSaga (was a
 * private nested class) and made a plain, Jackson-serializable bean as part of audit gap #7
 * remediation, so RedisGameStateStore can persist it as JSON — see GameStateStore.
 */
public class GameState {

    private String roomId;
    private Set<String> participants = new HashSet<>();
    private int currentRound = 0;
    private int totalRounds = 5;
    private Map<String, Integer> scores = new HashMap<>();
    private boolean ended = false;

    /** No-arg constructor required for Jackson deserialization (RedisGameStateStore). */
    public GameState() {}

    public GameState(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Set<String> getParticipants() { return participants; }
    public void setParticipants(Set<String> participants) { this.participants = participants; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public Map<String, Integer> getScores() { return scores; }
    public void setScores(Map<String, Integer> scores) { this.scores = scores; }
    public boolean isEnded() { return ended; }
    public void setEnded(boolean ended) { this.ended = ended; }
}
