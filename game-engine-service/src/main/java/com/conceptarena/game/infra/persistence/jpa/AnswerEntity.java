package com.conceptarena.game.infra.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "answers")
public class AnswerEntity {

    // Deterministic id = roundId + "::" + userId (assigned in RoundMapper) instead of a random
    // UUID: it makes RoundRepository.save idempotent (re-saving a round updates each answer row in
    // place rather than orphan-removing and re-inserting the whole set) and, crucially, turns the
    // "one answer per user per round" invariant into a PRIMARY KEY the database enforces — so two
    // requests from the same user racing on separate Round copies can't both insert (audit gap #1).
    @Id
    private String id;

    @Column(nullable = false)
    private String roundId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private String result;

    public AnswerEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
