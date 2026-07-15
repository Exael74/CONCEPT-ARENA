package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

/** See RoomJoined — same pattern, local re-publication after read-model consumption. */
public class RoomLeft extends DomainEvent {
    private final String userId;

    public RoomLeft(String roomId, String userId) {
        super(roomId);
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
