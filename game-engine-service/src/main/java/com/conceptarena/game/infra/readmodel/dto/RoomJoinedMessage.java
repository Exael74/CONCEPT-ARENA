package com.conceptarena.game.infra.readmodel.dto;

/** Mirrors room-service's RoomJoined domain event as serialized by its OutboxWritingEventHandler
 *  (whole-event Jackson serialization: eventId/occurredOn/aggregateId/userId). aggregateId is the roomId. */
public record RoomJoinedMessage(String eventId, String occurredOn, String aggregateId, String userId) {
}
