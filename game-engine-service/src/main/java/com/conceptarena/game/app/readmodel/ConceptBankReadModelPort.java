package com.conceptarena.game.app.readmodel;

/**
 * Replaces the monolith's direct ConceptBankRepository dependency in StartRoundCommandHandler.
 * Backed by a local read-model populated by consuming an enriched ConceptBankCreated (carrying
 * the full concept list) off RabbitMQ — see ADR-004.
 */
public interface ConceptBankReadModelPort {

    /** Throws IllegalStateException if the bank is unknown or has no concepts (same as the monolith's behavior). */
    ConceptSnapshot pickRandomConcept(String conceptBankId);

    record ConceptSnapshot(String question, String expectedAnswer, int difficulty) {}
}
