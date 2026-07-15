package com.conceptarena.game.infra.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "session_results")
public class SessionResultEntity {

    @Id
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "incorrect_answers", nullable = false)
    private int incorrectAnswers;

    @Column(name = "total_time_ms", nullable = false)
    private long totalTimeMs;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public SessionResultEntity() {}

    public SessionResultEntity(String id, String roomId, String userId, int totalPoints, int correctAnswers, int incorrectAnswers, long totalTimeMs, Instant completedAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.totalPoints = totalPoints;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.totalTimeMs = totalTimeMs;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getIncorrectAnswers() { return incorrectAnswers; }
    public void setIncorrectAnswers(int incorrectAnswers) { this.incorrectAnswers = incorrectAnswers; }

    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long totalTimeMs) { this.totalTimeMs = totalTimeMs; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
