package com.conceptarena.game.infra.timer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.RoundEndGuard;
import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.event.RoundEnded;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ScheduledTimerAdapterTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private RoundEndGuard roundEndGuard;
    @Mock private SimpMessagingTemplate messaging;

    private ScheduledTimerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ScheduledTimerAdapter(eventBus, roundRepository, roundEndGuard, messaging);
    }

    @Test
    void timeoutEndsTheRoundAndPublishesRoundEndedWhenItWinsTheClaim() {
        when(roundEndGuard.tryClaim("round-1")).thenReturn(true);
        Round round = new Round("room-1", "q", "a", 1, Duration.ofSeconds(30));
        round.start();
        when(roundRepository.findById("round-1")).thenReturn(Optional.of(round));
        when(roundRepository.save(any(Round.class))).thenReturn(round);

        adapter.endRoundOnTimeout("room-1", "round-1");

        verify(roundRepository).save(any(Round.class));
        verify(eventBus).publish(any(RoundEnded.class));
    }

    @Test
    void staleTimeoutDoesNothingWhenTheRoundWasAlreadyEnded() {
        // The early-end path already claimed this round; a late-firing timer for it must not
        // re-end it or publish a duplicate RoundEnded (which used to start a duplicate next round).
        when(roundEndGuard.tryClaim("round-1")).thenReturn(false);

        adapter.endRoundOnTimeout("room-1", "round-1");

        verify(roundRepository, never()).save(any(Round.class));
        verify(eventBus, never()).publish(any(RoundEnded.class));
    }
}
