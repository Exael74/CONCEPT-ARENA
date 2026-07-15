package com.conceptarena.conceptbank.domain;

import com.conceptarena.kernel.valueobject.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConceptBank {
    private final EntityId id;
    private final String name;
    private final String subject;
    private final List<Concept> concepts;

    private ConceptBank(EntityId id, String name, String subject, List<Concept> concepts) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.concepts = new ArrayList<>(concepts);
    }

    public static ConceptBank create(String name, String subject, List<Concept> concepts) {
        if (concepts == null || concepts.size() < 5) {
            throw new IllegalArgumentException("Concept bank must have at least 5 concepts");
        }
        return new ConceptBank(EntityId.generate(), name, subject, concepts);
    }

    public static ConceptBank restore(EntityId id, String name, String subject, List<Concept> concepts) {
        return new ConceptBank(id, name, subject, concepts);
    }

    public EntityId getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public List<Concept> getConcepts() { return Collections.unmodifiableList(concepts); }
    public int getConceptCount() { return concepts.size(); }
}
