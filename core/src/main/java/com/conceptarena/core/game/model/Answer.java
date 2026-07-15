package com.conceptarena.core.game.model;

import java.time.Instant;

public class Answer {
    private final String userId;
    private final String text;
    private final Instant submittedAt;
    private AnswerResult result;

    public enum AnswerResult {
        PENDING, CORRECT, INCORRECT, TIMEOUT
    }

    public Answer(String userId, String text) {
        this.userId = userId;
        this.text = text;
        this.submittedAt = Instant.now();
        this.result = AnswerResult.PENDING;
    }

    public Answer(String userId, String text, Instant submittedAt, AnswerResult result) {
        this.userId = userId;
        this.text = text;
        this.submittedAt = submittedAt;
        this.result = result;
    }

    public void markCorrect() { this.result = AnswerResult.CORRECT; }
    public void markIncorrect() { this.result = AnswerResult.INCORRECT; }
    public void markTimeout() { this.result = AnswerResult.TIMEOUT; }

    public String getUserId() { return userId; }
    public String getText() { return text; }
    public Instant getSubmittedAt() { return submittedAt; }
    public AnswerResult getResult() { return result; }
}
