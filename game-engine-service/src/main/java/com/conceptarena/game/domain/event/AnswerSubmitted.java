package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

public class AnswerSubmitted extends DomainEvent {
    private final String roomId;
    private final String userId;
    private final String answerText;
    private final String expectedAnswer;

    public AnswerSubmitted(String roundId, String roomId, String userId, String answerText, String expectedAnswer) {
        super(roundId);
        this.roomId = roomId;
        this.userId = userId;
        this.answerText = answerText;
        this.expectedAnswer = expectedAnswer;
    }

    public String getRoomId() { return roomId; }
    public String getUserId() { return userId; }
    public String getAnswerText() { return answerText; }
    public String getExpectedAnswer() { return expectedAnswer; }
}
