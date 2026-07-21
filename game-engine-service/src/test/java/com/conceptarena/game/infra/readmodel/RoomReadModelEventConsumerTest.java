package com.conceptarena.game.infra.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.domain.event.RoomJoined;
import com.conceptarena.game.domain.event.RoomLeft;
import com.conceptarena.game.infra.readmodel.dto.RoomCreatedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomJoinedMessage;
import com.conceptarena.game.infra.readmodel.dto.RoomLeftMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit-tests the listener method logic directly (as plain Java methods), bypassing RabbitMQ —
 * see the class-level comment on RoomReadModelEventConsumer for why (no broker is wired until a
 * later migration phase). Uses a real JPA context against H2 so the idempotency/upsert/
 * out-of-order behavior is verified against actual persistence, not a mocked repository (which
 * would hide constraint-violation bugs).
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Transactional
class RoomReadModelEventConsumerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private JpaRoomReadModelRepository roomRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private JpaParticipantReadModelRepository participantRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private JpaProcessedEventRepository processedEventRepository;

    @Mock private EventBus localEventBus;
    private RoomReadModelEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RoomReadModelEventConsumer(roomRepository, participantRepository, processedEventRepository, localEventBus, new ObjectMapper());
    }

    @Test
    void handleRoomCreatedUpsertsReadModelRow() {
        consumer.handleRoomCreated(new RoomCreatedMessage("room-1", "Study Room", "PUBLIC", "user-1", "bank-1", 4));

        RoomReadModelEntity entity = roomRepository.findById("room-1").orElseThrow();
        assertThat(entity.getConceptBankId()).isEqualTo("bank-1");
        assertThat(entity.getMaxParticipants()).isEqualTo(4);
    }

    @Test
    void handleRoomJoinedCreatesPlaceholderWhenRoomCreatedHasNotArrivedYet() {
        // Out-of-order redelivery: RoomJoined before RoomCreated must not be dropped.
        consumer.handleRoomJoined(new RoomJoinedMessage("evt-1", "2026-01-01T00:00:00Z", "room-2", "user-1"));

        assertThat(roomRepository.findById("room-2")).isPresent();
        assertThat(participantRepository.existsByRoomIdAndUserId("room-2", "user-1")).isTrue();
        verify(localEventBus).publish(any(RoomJoined.class));
    }

    @Test
    void handleRoomJoinedIsIdempotentUnderRedelivery() {
        RoomJoinedMessage msg = new RoomJoinedMessage("evt-1", "2026-01-01T00:00:00Z", "room-3", "user-1");

        consumer.handleRoomJoined(msg);
        consumer.handleRoomJoined(msg); // redelivery — must not throw a unique-constraint violation

        assertThat(participantRepository.existsByRoomIdAndUserId("room-3", "user-1")).isTrue();
    }

    @Test
    void handleRoomLeftRemovesParticipantAndRepublishesLocally() {
        consumer.handleRoomJoined(new RoomJoinedMessage("evt-1", "2026-01-01T00:00:00Z", "room-4", "user-1"));

        consumer.handleRoomLeft(new RoomLeftMessage("evt-2", "2026-01-01T00:01:00Z", "room-4", "user-1"));

        assertThat(participantRepository.existsByRoomIdAndUserId("room-4", "user-1")).isFalse();
        verify(localEventBus).publish(any(RoomLeft.class));
    }

    @Test
    void handleRoomCreatedIsIdempotentUnderRedelivery() {
        // RoomCreated has no eventId in its payload; it dedups on its natural key (room_id upsert).
        RoomCreatedMessage msg = new RoomCreatedMessage("room-5", "Study Room", "PUBLIC", "user-1", "bank-1", 4);

        consumer.handleRoomCreated(msg);
        consumer.handleRoomCreated(msg); // redelivery — must not duplicate the row or throw

        assertThat(roomRepository.findById("room-5").orElseThrow().getConceptBankId()).isEqualTo("bank-1");
        assertThat(roomRepository.count()).isEqualTo(1);
    }

    @Test
    void handleRoomLeftIsIdempotentUnderRedeliveryAndRepublishesOnlyOnce() {
        consumer.handleRoomJoined(new RoomJoinedMessage("evt-join", "2026-01-01T00:00:00Z", "room-6", "user-1"));
        RoomLeftMessage left = new RoomLeftMessage("evt-left", "2026-01-01T00:01:00Z", "room-6", "user-1");

        consumer.handleRoomLeft(left);
        consumer.handleRoomLeft(left); // redelivery — the eventId ledger must suppress the second effect

        assertThat(participantRepository.existsByRoomIdAndUserId("room-6", "user-1")).isFalse();
        // The local re-publish (which re-triggers GameSaga) must fire exactly once despite two deliveries.
        verify(localEventBus, times(1)).publish(any(RoomLeft.class));
    }
}
