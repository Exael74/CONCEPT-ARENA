package com.conceptarena.game.infra.persistence.jpa;

import jakarta.persistence.*;
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

    // Prevents the lost-update race that let a stale in-memory Round (loaded before a concurrent
    // request ended/transitioned this same round) blindly overwrite the row back to ACTIVE via a
    // later save() — which left 2 rounds simultaneously ACTIVE for one room, crashing every
    // subsequent findByRoomIdAndStatus lookup with IncorrectResultSizeDataAccessException (found in
    // production 2026-07-21: POST /api/game/{roomId}/answer 500s, and — even when it didn't throw —
    // could match a submitted answer against the WRONG round's expectedAnswer). Every save() now
    // must carry the version it was loaded with; a stale one is rejected with
    // ObjectOptimisticLockingFailureException instead of silently corrupting state — see
    // SubmitAnswerCommandHandler's handling of that exception.
    @Version
    private Long version;

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
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public List<AnswerEntity> getAnswers() { return answers; }
    public void setAnswers(List<AnswerEntity> answers) { this.answers = answers; }
}
