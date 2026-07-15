package com.conceptarena.game.infra.readmodel;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.infra.readmodel.dto.RoomCreatedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomJoinedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomLeftMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes room-service's RoomCreated/RoomJoined/RoomLeft (published via its outbox — see
 * docs/event-contracts.md) and maintains the local read-model tables. After updating the
 * read-model, RoomJoined/RoomLeft are re-published on the LOCAL event bus so GameSaga's
 * unchanged subscription logic keeps working — see ADR-004.
 *
 * Each handle* method is idempotent (upsert / delete-if-exists) so at-least-once redelivery
 * never throws a constraint violation that would wedge the consumer in a requeue loop. A
 * RoomJoined arriving before its RoomCreated (out-of-order redelivery) creates a placeholder
 * room row rather than dropping the event.
 *
 * Not wired to a real broker until RabbitMQ is stood up — connection attempts fail/retry safely
 * in the meantime (same behavior already exercised by every service's OutboxEventPublisher).
 * The listener methods are unit-tested directly (as plain Java methods) rather than through a
 * live broker for that reason.
 */
@Component
public class RoomReadModelEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RoomReadModelEventConsumer.class);

    private final JpaRoomReadModelRepository roomRepository;
    private final JpaParticipantReadModelRepository participantRepository;
    private final EventBus localEventBus;
    private final ObjectMapper objectMapper;

    public RoomReadModelEventConsumer(JpaRoomReadModelRepository roomRepository,
                                       JpaParticipantReadModelRepository participantRepository,
                                       EventBus localEventBus, ObjectMapper objectMapper) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.localEventBus = localEventBus;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "game-engine.room.created.readmodel")
    public void onRoomCreatedMessage(String rawPayload) throws Exception {
        handleRoomCreated(objectMapper.readValue(rawPayload, RoomCreatedMessage.class));
    }

    @Transactional
    public void handleRoomCreated(RoomCreatedMessage msg) {
        RoomReadModelEntity entity = roomRepository.findById(msg.roomId()).orElseGet(RoomReadModelEntity::new);
        entity.setRoomId(msg.roomId());
        entity.setConceptBankId(msg.conceptBankId());
        entity.setMaxParticipants(msg.maxParticipants());
        roomRepository.save(entity);
        log.debug("Read-model: room {} created (conceptBankId={})", msg.roomId(), msg.conceptBankId());
    }

    @RabbitListener(queues = "game-engine.room.joined.readmodel")
    public void onRoomJoinedMessage(String rawPayload) throws Exception {
        handleRoomJoined(objectMapper.readValue(rawPayload, RoomJoinedMessage.class));
    }

    @Transactional
    public void handleRoomJoined(RoomJoinedMessage msg) {
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
    }

    @RabbitListener(queues = "game-engine.room.left.readmodel")
    public void onRoomLeftMessage(String rawPayload) throws Exception {
        handleRoomLeft(objectMapper.readValue(rawPayload, RoomLeftMessage.class));
    }

    @Transactional
    public void handleRoomLeft(RoomLeftMessage msg) {
        String roomId = msg.aggregateId();
        participantRepository.deleteByRoomIdAndUserId(roomId, msg.userId());
        localEventBus.publish(new com.conceptarena.game.domain.event.RoomLeft(roomId, msg.userId()));
    }
}
