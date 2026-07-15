package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

public class RoundStarted extends DomainEvent {
    private final String roomId;
    private final String conceptQuestion;
    private final int difficulty;
    private final int durationSeconds;

    public RoundStarted(String roundId, String roomId, String conceptQuestion, int difficulty, int durationSeconds) {
        super(roundId);
        this.roomId = roomId;
        this.conceptQuestion = conceptQuestion;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
    }

    public String getRoomId() { return roomId; }
    public String getConceptQuestion() { return conceptQuestion; }
    public int getDifficulty() { return difficulty; }
    public int getDurationSeconds() { return durationSeconds; }
}
