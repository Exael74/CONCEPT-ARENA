package com.conceptarena.conceptbank.domain.event;

import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.kernel.event.DomainEvent;
import java.util.List;

/**
 * Carries the full concept list (not just name/subject, unlike the monolith's original payload)
 * so game-engine-service can build a local read-model from this event alone, without a synchronous
 * call back to concept-bank-service on every round start. See
 * docs/architecture-decisions/ADR-004-game-engine-read-models.md. Reasonable because banks are
 * small and effectively immutable after creation (no UpdateConceptBankCommand exists).
 */
public class ConceptBankCreated extends DomainEvent {
    private final String name;
    private final String subject;
    private final List<ConceptSnapshot> concepts;

    public ConceptBankCreated(String bankId, String name, String subject, List<Concept> concepts) {
        super(bankId);
        this.name = name;
        this.subject = subject;
        this.concepts = concepts.stream()
            .map(c -> new ConceptSnapshot(c.getQuestion(), c.getExpectedAnswer(), c.getDifficulty()))
            .toList();
    }

    public String getName() { return name; }
    public String getSubject() { return subject; }
    public List<ConceptSnapshot> getConcepts() { return concepts; }

    public record ConceptSnapshot(String question, String expectedAnswer, int difficulty) {}
}
