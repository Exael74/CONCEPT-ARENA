package com.conceptarena.game.infra.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private String id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String routingKey;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Lob
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(String id, String aggregateId, String eventType, String exchange,
                        String routingKey, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public String getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getExchange() { return exchange; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public void markPublished(Instant when) {
        this.publishedAt = when;
    }

    public void recordFailure(String errorMessage) {
        this.attempts++;
        this.lastError = errorMessage;
    }
}
