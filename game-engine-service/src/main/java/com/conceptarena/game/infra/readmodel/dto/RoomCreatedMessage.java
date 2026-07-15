package com.conceptarena.game.infra.readmodel.dto;

/** Mirrors room-service's OutboxWritingEventHandler.RoomCreatedPayload — see docs/event-contracts.md. */
public record RoomCreatedMessage(
    String roomId, String name, String type, String creatorUserId, String conceptBankId, int maxParticipants) {
}
