package com.conceptarena.app.game;

public interface AnswerValidationPort {
    boolean isCorrect(String answerText, String expectedAnswer);
}
