package com.conceptarena.core.voice.event;

import com.conceptarena.core.shared.event.DomainEvent;

public class VoiceDisconnected extends DomainEvent {
    private final String roomId;
    private final String userId;

    public VoiceDisconnected(String sessionId, String roomId, String userId) {
        super(sessionId);
        this.roomId = roomId;
        this.userId = userId;
    }

    public String getRoomId() { return roomId; }
    public String getUserId() { return userId; }
}
