package com.conceptarena.core.voice.model;

import com.conceptarena.core.shared.valueobject.EntityId;

public class VoiceSession {
    private final EntityId id;
    private final String roomId;
    private final String userId;
    private boolean connected;

    public VoiceSession(String roomId, String userId) {
        this.id = EntityId.generate();
        this.roomId = roomId;
        this.userId = userId;
        this.connected = false;
    }

    public void connect() { this.connected = true; }
    public void disconnect() { this.connected = false; }

    public EntityId getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getUserId() { return userId; }
    public boolean isConnected() { return connected; }
}
