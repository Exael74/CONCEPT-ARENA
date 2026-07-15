package com.conceptarena.game.infra.readmodel.dto;

import java.util.List;

/** Mirrors concept-bank-service's enriched ConceptBankCreated event — see ADR-004/event-contracts.md. */
public record ConceptBankCreatedMessage(
    String eventId, String occurredOn, String aggregateId, String name, String subject, List<ConceptSnapshotMessage> concepts) {

    public record ConceptSnapshotMessage(String question, String expectedAnswer, int difficulty) {}
}
