package com.conceptarena.conceptbank.infra.persistence.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "concepts")
public class ConceptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String bankId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 2000)
    private String expectedAnswer;

    @Column(nullable = false)
    private int difficulty;

    public ConceptEntity() {}

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
