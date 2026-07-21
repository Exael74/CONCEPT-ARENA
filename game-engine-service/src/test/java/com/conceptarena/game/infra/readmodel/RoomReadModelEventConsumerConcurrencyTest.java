package com.conceptarena.game.infra.readmodel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.bus.EventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Found by actually running against RabbitMQ (audit gap #2/#5 remediation): RoomCreated and
 * RoomJoined for the same room are consumed by two different listener threads concurrently
 * (different queues), and the "check row absent, then insert" idempotency pattern races — the
 * loser's insert violates the room_read_model primary key at commit time. This test locks in the
 * fix: onRoomJoinedMessage's withCorrelationId wrapper retries once when that specific exception
 * surfaces, and the retry must succeed (not throw) once the "winning" insert would have committed.
 */
class RoomReadModelEventConsumerConcurrencyTest {

    @Test
    void onRoomJoinedMessageRetriesOnceAfterConcurrentInsertRaceAndSucceeds() throws Exception {
        JpaRoomReadModelRepository roomRepository = mock(JpaRoomReadModelRepository.class);
        JpaParticipantReadModelRepository participantRepository = mock(JpaParticipantReadModelRepository.class);
        JpaProcessedEventRepository processedEvents = mock(JpaProcessedEventRepository.class);
        EventBus localEventBus = mock(EventBus.class);
        RoomReadModelEventConsumer consumer =
            new RoomReadModelEventConsumer(roomRepository, participantRepository, processedEvents, localEventBus, new ObjectMapper());

        // First attempt: room row looks absent (the race window), so the consumer tries to
        // insert a placeholder — that's exactly where the concurrent winning transaction's
        // commit would surface as a constraint violation in the real DB.
        when(roomRepository.findById("room-1"))
            .thenReturn(Optional.empty())   // first attempt sees it as absent...
            .thenReturn(Optional.of(new RoomReadModelEntity())); // ...retry sees the winner's committed row
        doThrow(new DataIntegrityViolationException("duplicate key"))
            .when(roomRepository).save(any());

        String payload = "{\"eventId\":\"evt-1\",\"occurredOn\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"room-1\",\"userId\":\"user-1\"}";

        // Must not throw: the retry inside withCorrelationId should absorb the race and succeed.
        consumer.onRoomJoinedMessage(payload, "corr-1");

        // First attempt tried to save the placeholder (and "failed"); retry skipped that insert
        // (room now exists per the second findById stub) and only inserted the participant.
        verify(roomRepository, times(1)).save(any());
        verify(participantRepository, times(1)).save(any());
    }
}
