package com.conceptarena.game.infra.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "answers")
public class AnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
