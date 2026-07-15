package com.conceptarena.game.infra.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "concept_read_model")
public class ConceptReadModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "bank_id", nullable = false)
    private String bankId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 2000)
    private String expectedAnswer;

    @Column(nullable = false)
    private int difficulty;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
}
