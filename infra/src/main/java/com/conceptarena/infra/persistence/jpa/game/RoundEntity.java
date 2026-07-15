package com.conceptarena.infra.persistence.jpa.game;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String conceptQuestion;

    @Column(nullable = false, length = 1000)
    private String expectedAnswer;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private long durationSeconds;

    @Column(nullable = false)
    private String status;

    private Instant startedAt;
    private Instant endedAt;

    @OneToMany(mappedBy = "roundId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerEntity> answers = new ArrayList<>();

    public RoundEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getConceptQuestion() { return conceptQuestion; }
    public void setConceptQuestion(String conceptQuestion) { this.conceptQuestion = conceptQuestion; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public List<AnswerEntity> getAnswers() { return answers; }
    public void setAnswers(List<AnswerEntity> answers) { this.answers = answers; }
}
