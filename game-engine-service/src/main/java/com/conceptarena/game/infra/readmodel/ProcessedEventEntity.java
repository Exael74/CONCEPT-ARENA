package com.conceptarena.game.infra.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Idempotency-ledger row (audit gap #6): one row per already-applied cross-service eventId. See
 * RoomReadModelEventConsumer — a redelivered RoomJoined/RoomLeft whose eventId is already recorded
 * here is skipped, so its local-bus re-publish (and the GameSaga effect behind it) runs at most once.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    private String eventId;

    @Column(nullable = false)
    private Instant consumedAt;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String eventId) {
        this.eventId = eventId;
        this.consumedAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
