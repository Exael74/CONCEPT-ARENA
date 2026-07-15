package com.conceptarena.core.concept.event;

import com.conceptarena.core.shared.event.DomainEvent;

public class ConceptBankCreated extends DomainEvent {
    private final String name;
    private final String subject;

    public ConceptBankCreated(String bankId, String name, String subject) {
        super(bankId);
        this.name = name;
        this.subject = subject;
    }

    public String getName() { return name; }
    public String getSubject() { return subject; }
}
