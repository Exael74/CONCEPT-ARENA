package com.conceptarena.room.infra.messaging.outbox;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.app.bus.EventHandler;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import com.conceptarena.room.domain.event.RoomLeft;
import com.conceptarena.room.infra.security.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * See docs/event-contracts.md and ADR-002/ADR-003. Write ordering: each command handler already
 * calls roomRepository.save(room) (Redis) BEFORE eventBus.publish(...) — so by the time this
 * handler runs and inserts the outbox row, the Redis write has already succeeded. If this insert
 * fails, the room state is still correct in Redis; only the event's publication is lost — an
 * accepted, documented risk (see ADR-003), not a data-corruption risk.
 */
@Component
public class OutboxWritingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxWritingEventHandler.class);
    private static final String EXCHANGE = "conceptarena.room.events";

    private final EventBus eventBus;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxWritingEventHandler(EventBus eventBus, OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper) {
        this.eventBus = eventBus;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
    }

    private void onRoomCreated(RoomCreated event) {
        // inviteCode is deliberately excluded from the cross-service payload — it stays private
        // to room-service (see docs/event-contracts.md); game-engine's read-model doesn't need it.
        var payload = new RoomCreatedPayload(
            event.getAggregateId(), event.getName(), event.getType().name(),
            event.getCreatorUserId(), event.getConceptBankId(), event.getMaxParticipants());
        write(event.getAggregateId(), "RoomCreated", "room.room-created", payload);
    }

    private void onRoomJoined(RoomJoined event) {
        write(event.getAggregateId(), "RoomJoined", "room.room-joined", event);
    }

    private void onRoomLeft(RoomLeft event) {
        write(event.getAggregateId(), "RoomLeft", "room.room-left", event);
    }

    private void write(String aggregateId, String eventType, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent row = new OutboxEvent(
                UUID.randomUUID().toString(), aggregateId, eventType, EXCHANGE, routingKey, json, Instant.now(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
            outboxEventRepository.save(row);
        } catch (Exception e) {
            log.error("Failed to write outbox row for {} (aggregateId={}): {}", eventType, aggregateId, e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize event for outbox: " + eventType, e);
        }
    }

    private record RoomCreatedPayload(
        String roomId, String name, String type, String creatorUserId, String conceptBankId, int maxParticipants) {
    }
}
