package com.conceptarena.conceptbank.infra.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Transactional outbox row — written in the same DB transaction as the domain change that
 * produced the event, and later relayed to RabbitMQ by {@link OutboxEventPublisher}. See
 * docs/architecture-decisions/ADR-002-outbox-pattern-scheduled-polling.md.
 */
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

    /** The originating HTTP request's X-Request-Id (see CorrelationIdFilter), captured from MDC
     *  at write time so consumers on the other side of RabbitMQ can link their logs back to it —
     *  see docs/event-contracts.md, audit gap #5. Null for events with no HTTP request context. */
    private String correlationId;

    protected OutboxEvent() {}

    public OutboxEvent(String id, String aggregateId, String eventType, String exchange,
                        String routingKey, String payload, Instant createdAt, String correlationId) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
        this.correlationId = correlationId;
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
    public String getCorrelationId() { return correlationId; }

    public void markPublished(Instant when) {
        this.publishedAt = when;
    }

    public void recordFailure(String errorMessage) {
        this.attempts++;
        this.lastError = errorMessage;
    }
}
