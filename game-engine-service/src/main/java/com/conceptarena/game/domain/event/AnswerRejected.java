package com.conceptarena.game.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

/**
 * Published whenever SubmitAnswerCommandHandler rejects a submission, so the rejection
 * (a potential cheat attempt or a client bug) is visible as a metric instead of only
 * surfacing as an HTTP/WS error the server never records anywhere.
 */
public class AnswerRejected extends DomainEvent {
    private final String roomId;
    private final String userId;
    private final String reason;

    public AnswerRejected(String roomId, String userId, String reason) {
        super(roomId);
        this.roomId = roomId;
        this.userId = userId;
        this.reason = reason;
    }

    public String getRoomId() { return roomId; }
    public String getUserId() { return userId; }
    public String getReason() { return reason; }
}
