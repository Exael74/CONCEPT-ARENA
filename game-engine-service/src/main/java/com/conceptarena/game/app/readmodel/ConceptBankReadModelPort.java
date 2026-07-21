package com.conceptarena.game.app.readmodel;

import java.util.Optional;

/**
 * Replaces the monolith's direct ConceptBankRepository dependency in StartRoundCommandHandler.
 * Backed by a local read-model populated by consuming an enriched ConceptBankCreated (carrying
 * the full concept list) off RabbitMQ — see ADR-004.
 */
public interface ConceptBankReadModelPort {

    /**
     * Empty if the bank is unknown or has no concepts. Deliberately NOT a thrown exception: the
     * implementation is a Spring-proxied @Repository bean, and Spring's persistence-exception
     * translation silently rewraps a bare IllegalStateException/IllegalArgumentException thrown
     * from such a bean into InvalidDataAccessApiUsageException — which is NOT an IllegalStateException,
     * so it slipped past GameController's `catch (IllegalArgumentException | IllegalStateException)`
     * and surfaced as a 500 instead of the intended 400. The caller (StartRoundCommandHandler, a
     * plain @Service) throws IllegalStateException itself on empty, where no such translation applies.
     */
    Optional<ConceptSnapshot> pickRandomConcept(String conceptBankId);

    record ConceptSnapshot(String question, String expectedAnswer, int difficulty) {}
}
