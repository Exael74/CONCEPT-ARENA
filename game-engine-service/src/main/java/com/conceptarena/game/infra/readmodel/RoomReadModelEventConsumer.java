package com.conceptarena.game.infra.readmodel;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.infra.readmodel.dto.RoomCreatedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomJoinedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomLeftMessage;
import com.conceptarena.game.infra.security.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes room-service's RoomCreated/RoomJoined/RoomLeft (published via its outbox — see
 * docs/event-contracts.md) and maintains the local read-model tables. After updating the
 * read-model, RoomJoined/RoomLeft are re-published on the LOCAL event bus so GameSaga's
 * unchanged subscription logic keeps working — see ADR-004.
 *
 * Each handle* method is idempotent: RoomJoined/RoomLeft dedup on an eventId ledger
 * (processed_events, audit gap #6) so a redelivery does not re-run the local-bus re-publish that
 * drives GameSaga; RoomCreated has no eventId in its payload and is a natural-key upsert on
 * room_id. That idempotency also means at-least-once redelivery never throws a constraint
 * violation that would wedge the consumer in a requeue loop. A RoomJoined arriving before its
 * RoomCreated (out-of-order redelivery) creates a placeholder room row rather than dropping it.
 *
 * Found by actually running against RabbitMQ (audit gap #2 remediation, 2026-07-15) — the exact
 * risk the old "not wired to a real broker" comment on this class glossed over: RoomCreated and
 * RoomJoined for the same room (CreateRoomCommandHandler publishes both for the creator) land on
 * two DIFFERENT queues, consumed by two DIFFERENT listener container threads CONCURRENTLY. Both
 * handleRoomCreated and handleRoomJoined's "check row absent, then insert" is a TOCTOU race: both
 * threads can see "absent" before either commits, and the loser's INSERT then violates the
 * room_read_model primary key at commit time — which also rolls back that same transaction's
 * participant insert, not just the room row. withCorrelationId retries the whole handler once on
 * that specific exception: by the time the retry runs, the winning thread's insert has committed,
 * so the retry's existence-checks see it and cleanly do only the remaining (participant) insert.
 */
@Component
public class RoomReadModelEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RoomReadModelEventConsumer.class);

    private final JpaRoomReadModelRepository roomRepository;
    private final JpaParticipantReadModelRepository participantRepository;
    private final JpaProcessedEventRepository processedEvents;
    private final EventBus localEventBus;
    private final ObjectMapper objectMapper;

    public RoomReadModelEventConsumer(JpaRoomReadModelRepository roomRepository,
                                       JpaParticipantReadModelRepository participantRepository,
                                       JpaProcessedEventRepository processedEvents,
                                       EventBus localEventBus, ObjectMapper objectMapper) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.processedEvents = processedEvents;
        this.localEventBus = localEventBus;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "game-engine.room.created.readmodel")
    public void onRoomCreatedMessage(String rawPayload,
                                      @Header(value = "correlationId", required = false) String correlationId) throws Exception {
        withCorrelationId(correlationId, () -> handleRoomCreated(objectMapper.readValue(rawPayload, RoomCreatedMessage.class)));
    }

    @Transactional
    public void handleRoomCreated(RoomCreatedMessage msg) {
        RoomReadModelEntity entity = roomRepository.findById(msg.roomId()).orElseGet(RoomReadModelEntity::new);
        entity.setRoomId(msg.roomId());
        entity.setCreatorUserId(msg.creatorUserId());
        entity.setConceptBankId(msg.conceptBankId());
        entity.setMaxParticipants(msg.maxParticipants());
        roomRepository.save(entity);
        log.debug("Read-model: room {} created (conceptBankId={})", msg.roomId(), msg.conceptBankId());
    }

    @RabbitListener(queues = "game-engine.room.joined.readmodel")
    public void onRoomJoinedMessage(String rawPayload,
                                     @Header(value = "correlationId", required = false) String correlationId) throws Exception {
        withCorrelationId(correlationId, () -> handleRoomJoined(objectMapper.readValue(rawPayload, RoomJoinedMessage.class)));
    }

    @Transactional
    public void handleRoomJoined(RoomJoinedMessage msg) {
        if (alreadyProcessed(msg.eventId())) {
            return;
        }
        String roomId = msg.aggregateId();
        if (roomRepository.findById(roomId).isEmpty()) {
            // Out-of-order redelivery: RoomJoined arrived before RoomCreated. Placeholder row,
            // filled in properly once/if RoomCreated arrives.
            RoomReadModelEntity placeholder = new RoomReadModelEntity();
            placeholder.setRoomId(roomId);
            roomRepository.save(placeholder);
        }
        if (!participantRepository.existsByRoomIdAndUserId(roomId, msg.userId())) {
            ParticipantReadModelEntity participant = new ParticipantReadModelEntity();
            participant.setRoomId(roomId);
            participant.setUserId(msg.userId());
            participantRepository.save(participant);
        }
        localEventBus.publish(new com.conceptarena.game.domain.event.RoomJoined(roomId, msg.userId()));
        markProcessed(msg.eventId());
    }

    /**
     * Audit gap #6: RoomJoined/RoomLeft re-publish on the local bus (re-triggering GameSaga) on
     * every delivery, so an at-least-once redelivery would double-apply the effect. Skipping any
     * eventId already in the ledger makes each event's effect fire at most once. Runs in the same
     * transaction as the effect: on a concurrent duplicate both handlers can pass this check, but
     * the losing markProcessed collides on the event_id primary key and the resulting
     * DataIntegrityViolationException routes through withCorrelationId's retry — which then sees
     * the ledger row and skips cleanly.
     */
    private boolean alreadyProcessed(String eventId) {
        if (eventId != null && processedEvents.existsById(eventId)) {
            log.debug("Read-model: event {} already processed, skipping redelivery", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventId) {
        if (eventId != null) {
            processedEvents.save(new ProcessedEventEntity(eventId));
        }
    }

    @RabbitListener(queues = "game-engine.room.left.readmodel")
    public void onRoomLeftMessage(String rawPayload,
                                   @Header(value = "correlationId", required = false) String correlationId) throws Exception {
        withCorrelationId(correlationId, () -> handleRoomLeft(objectMapper.readValue(rawPayload, RoomLeftMessage.class)));
    }

    /**
     * Audit gap #5 remediation: puts the correlationId carried on the RabbitMQ message header
     * (set by OutboxEventPublisher from the originating HTTP request's X-Request-Id) into this
     * thread's MDC under the same key CorrelationIdFilter uses, so this consumer's logs are
     * linkable back to the request that caused the event — see docs/event-contracts.md.
     */
    private void withCorrelationId(String correlationId, RunnableWithException action) throws Exception {
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            try {
                action.run();
            } catch (DataIntegrityViolationException raceLostOnFirstAttempt) {
                log.debug("Read-model: concurrent-insert race on first attempt, retrying once now "
                    + "that the winning transaction has committed: {}", raceLostOnFirstAttempt.getMessage());
                action.run();
            }
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    @Transactional
    public void handleRoomLeft(RoomLeftMessage msg) {
        if (alreadyProcessed(msg.eventId())) {
            return;
        }
        String roomId = msg.aggregateId();
        participantRepository.deleteByRoomIdAndUserId(roomId, msg.userId());
        localEventBus.publish(new com.conceptarena.game.domain.event.RoomLeft(roomId, msg.userId()));
        markProcessed(msg.eventId());
    }
}
