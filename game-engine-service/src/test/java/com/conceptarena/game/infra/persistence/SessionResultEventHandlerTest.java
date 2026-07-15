package com.conceptarena.game.infra.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.infra.persistence.jpa.SessionResultEntity;
import com.conceptarena.game.infra.persistence.jpa.SpringDataSessionResultRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the fix for the GameEnded idempotency gap: without a dedup guard, replaying the
 * same GameEnded event (e.g. from the RoundEnded double-publish race, or now also possible
 * at-least-once RabbitMQ redelivery once GameEnded crosses a real broker) would insert a
 * second SessionResultEntity row per user and double-count KPIs.
 */
@ExtendWith(MockitoExtension.class)
class SessionResultEventHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private SpringDataSessionResultRepository sessionResultRepository;

    private EventHandler<GameEnded> gameEndedHandler;

    @BeforeEach
    void setUp() {
        SessionResultEventHandler handler = new SessionResultEventHandler(eventBus, roundRepository, sessionResultRepository);
        handler.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventHandler<GameEnded>> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(eventBus).subscribe(eq(GameEnded.class), captor.capture());
        gameEndedHandler = captor.getValue();
    }

    @Test
    void persistsSessionResultOnFirstGameEnded() {
        when(roundRepository.findByRoomId("room-1")).thenReturn(List.of());
        when(sessionResultRepository.existsByRoomIdAndUserId("room-1", "user-1")).thenReturn(false);

        gameEndedHandler.handle(new GameEnded("room-1", Map.of("user-1", 42)));

        verify(sessionResultRepository, times(1)).save(any(SessionResultEntity.class));
    }

    @Test
    void doesNotDuplicateSessionResultWhenTheSameGameEndedIsProcessedTwice() {
        when(roundRepository.findByRoomId("room-1")).thenReturn(List.of());
        GameEnded event = new GameEnded("room-1", Map.of("user-1", 42));

        when(sessionResultRepository.existsByRoomIdAndUserId("room-1", "user-1")).thenReturn(false);
        gameEndedHandler.handle(event);

        // Second delivery of the identical event: the guard must now see the row as existing.
        when(sessionResultRepository.existsByRoomIdAndUserId("room-1", "user-1")).thenReturn(true);
        gameEndedHandler.handle(event);

        verify(sessionResultRepository, times(1)).save(any(SessionResultEntity.class));
    }
}
