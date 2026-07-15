package com.conceptarena.core.concept.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConceptBankTest {

    private Concept concept(String question) {
        return new Concept(question, "answer", 1);
    }

    @Test
    void createRejectsFewerThanFiveConcepts() {
        List<Concept> fourConcepts = List.of(concept("q1"), concept("q2"), concept("q3"), concept("q4"));
        assertThatThrownBy(() -> ConceptBank.create("Bank", "Subject", fourConcepts))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createAcceptsExactlyFiveConcepts() {
        List<Concept> fiveConcepts = List.of(
            concept("q1"), concept("q2"), concept("q3"), concept("q4"), concept("q5"));
        ConceptBank bank = ConceptBank.create("Bank", "Subject", fiveConcepts);
        assertThat(bank.getConceptCount()).isEqualTo(5);
    }
}
