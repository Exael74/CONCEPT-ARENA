package com.conceptarena.game.infra.readmodel.dto;

/** See RoomJoinedMessage — same shape. */
public record RoomLeftMessage(String eventId, String occurredOn, String aggregateId, String userId) {
}
