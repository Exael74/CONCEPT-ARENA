package com.conceptarena.game.infra.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Local copy of concept-bank-service's ConceptBank, populated from an enriched ConceptBankCreated. */
@Entity
@Table(name = "conceptbank_read_model")
public class ConceptBankReadModelEntity {

    @Id
    private String bankId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}
