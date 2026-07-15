package com.conceptarena.conceptbank.infra.persistence.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concept_banks")
public class ConceptBankEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    @OneToMany(mappedBy = "bankId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConceptEntity> concepts = new ArrayList<>();

    public ConceptBankEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public List<ConceptEntity> getConcepts() { return concepts; }
    public void setConcepts(List<ConceptEntity> concepts) { this.concepts = concepts; }
}
