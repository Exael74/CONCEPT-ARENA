package com.conceptarena.core.game.event;

import com.conceptarena.core.shared.event.DomainEvent;
import java.util.Map;

public class GameEnded extends DomainEvent {
    private final String roomId;
    private final Map<String, Integer> finalScores;

    public GameEnded(String roomId, Map<String, Integer> finalScores) {
        super(roomId);
        this.roomId = roomId;
        this.finalScores = finalScores;
    }

    public String getRoomId() { return roomId; }
    public Map<String, Integer> getFinalScores() { return finalScores; }
}
