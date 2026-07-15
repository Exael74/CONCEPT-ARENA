package com.conceptarena.game.app;

public interface AnswerValidationPort {
    boolean isCorrect(String answerText, String expectedAnswer);
}
