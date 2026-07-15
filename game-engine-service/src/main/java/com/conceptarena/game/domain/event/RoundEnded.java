package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;
import java.util.Map;

public class RoundEnded extends DomainEvent {
    private final String roomId;
    private final Map<String, Integer> scores;
    private final Map<String, String> results;

    public RoundEnded(String roundId, String roomId, Map<String, Integer> scores, Map<String, String> results) {
        super(roundId);
        this.roomId = roomId;
        this.scores = scores;
        this.results = results;
    }

    public String getRoomId() { return roomId; }
    public Map<String, Integer> getScores() { return scores; }
    public Map<String, String> getResults() { return results; }
}
