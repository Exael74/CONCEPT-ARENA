package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

/**
 * Local re-publication of room-service's RoomJoined event, after RoomReadModelEventConsumer has
 * updated the local read-model tables from the RabbitMQ message — see ADR-004. GameSaga
 * subscribes to this exactly as it subscribed to the monolith's shared in-process RoomJoined,
 * unchanged.
 */
public class RoomJoined extends DomainEvent {
    private final String userId;

    public RoomJoined(String roomId, String userId) {
        super(roomId);
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
