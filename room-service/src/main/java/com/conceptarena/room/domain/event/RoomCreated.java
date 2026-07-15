package com.conceptarena.room.domain.event;

import com.conceptarena.room.domain.RoomType;
import com.conceptarena.kernel.event.DomainEvent;

/**
 * Enriched with conceptBankId/maxParticipants (the monolith's version only carried
 * name/type/inviteCode/creatorUserId) so game-engine-service's read-model consumer can populate
 * a full RoomSnapshot from this event alone — see ADR-004. inviteCode stays on this in-process
 * domain event (needed by same-process consumers) but is deliberately excluded from the
 * cross-service outbox payload by OutboxWritingEventHandler — see docs/event-contracts.md.
 */
public class RoomCreated extends DomainEvent {
    private final String name;
    private final RoomType type;
    private final String inviteCode;
    private final String creatorUserId;
    private final String conceptBankId;
    private final int maxParticipants;

    public RoomCreated(String roomId, String name, RoomType type, String inviteCode, String creatorUserId,
                        String conceptBankId, int maxParticipants) {
        super(roomId);
        this.name = name;
        this.type = type;
        this.inviteCode = inviteCode;
        this.creatorUserId = creatorUserId;
        this.conceptBankId = conceptBankId;
        this.maxParticipants = maxParticipants;
    }

    public String getName() { return name; }
    public RoomType getType() { return type; }
    public String getInviteCode() { return inviteCode; }
    public String getCreatorUserId() { return creatorUserId; }
    public String getConceptBankId() { return conceptBankId; }
    public int getMaxParticipants() { return maxParticipants; }
}
