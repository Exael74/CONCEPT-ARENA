package com.conceptarena.core.room.event;

import com.conceptarena.core.shared.event.DomainEvent;

public class RoomLeft extends DomainEvent {
    private final String userId;

    public RoomLeft(String roomId, String userId) {
        super(roomId);
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
