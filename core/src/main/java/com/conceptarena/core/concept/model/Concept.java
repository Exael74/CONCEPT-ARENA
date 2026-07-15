package com.conceptarena.core.concept.model;

public class Concept {
    private final String question;
    private final String expectedAnswer;
    private final int difficulty;

    public Concept(String question, String expectedAnswer, int difficulty) {
        this.question = question;
        this.expectedAnswer = expectedAnswer;
        this.difficulty = difficulty;
    }

    public String getQuestion() { return question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public int getDifficulty() { return difficulty; }
}
