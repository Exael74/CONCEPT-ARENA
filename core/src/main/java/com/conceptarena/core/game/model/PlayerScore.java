package com.conceptarena.core.game.model;

public class PlayerScore implements Comparable<PlayerScore> {
    private final String userId;
    private int totalPoints;
    private int correctAnswers;
    private int incorrectAnswers;
    private long totalTimeMs;

    public PlayerScore(String userId) {
        this.userId = userId;
        this.totalPoints = 0;
        this.correctAnswers = 0;
        this.incorrectAnswers = 0;
        this.totalTimeMs = 0;
    }

    public void addCorrectAnswer(int points, long timeMs) {
        this.totalPoints += points;
        this.correctAnswers++;
        this.totalTimeMs += timeMs;
    }

    public void addIncorrectAnswer() {
        this.incorrectAnswers++;
    }

    @Override
    public int compareTo(PlayerScore other) {
        return Integer.compare(other.totalPoints, this.totalPoints);
    }

    public String getUserId() { return userId; }
    public int getTotalPoints() { return totalPoints; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getIncorrectAnswers() { return incorrectAnswers; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public double getAverageTimeMs() {
        return correctAnswers > 0 ? (double) totalTimeMs / correctAnswers : 0;
    }
}
