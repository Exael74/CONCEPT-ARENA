package com.conceptarena.core.room.event;

import com.conceptarena.core.room.model.RoomType;
import com.conceptarena.core.shared.event.DomainEvent;

public class RoomCreated extends DomainEvent {
    private final String name;
    private final RoomType type;
    private final String inviteCode;
    private final String creatorUserId;

    public RoomCreated(String roomId, String name, RoomType type, String inviteCode, String creatorUserId) {
        super(roomId);
        this.name = name;
        this.type = type;
        this.inviteCode = inviteCode;
        this.creatorUserId = creatorUserId;
    }

    public String getName() { return name; }
    public RoomType getType() { return type; }
    public String getInviteCode() { return inviteCode; }
    public String getCreatorUserId() { return creatorUserId; }
}
