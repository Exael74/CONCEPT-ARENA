package com.conceptarena.game.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoomJoined;
import com.conceptarena.game.domain.event.RoomLeft;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import com.conceptarena.kernel.event.DomainEvent;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the fix for the early-round-end race condition: when all participants
 * answer before the scheduled timer fires, GameSaga must cancel that timer via
 * TimerPort so a stale RoundEnded doesn't fire twice for the same round.
 */
@ExtendWith(MockitoExtension.class)
class GameSagaTest {

    @Mock private EventBus eventBus;
    @Mock private CommandBus commandBus;
    @Mock private RoundRepository roundRepository;
    @Mock private TimerPort timerPort;
    private final RoundEndGuard roundEndGuard = new RoundEndGuard();

    private EventHandler<RoomJoined> roomJoinedHandler;
    private EventHandler<RoomLeft> roomLeftHandler;
    private EventHandler<RoundStarted> roundStartedHandler;
    private EventHandler<AnswerSubmitted> answerSubmittedHandler;
    private EventHandler<RoundEnded> roundEndedHandler;

    @BeforeEach
    void setUp() {
        GameSaga saga = new GameSaga(eventBus, commandBus, roundRepository, timerPort, roundEndGuard);
        saga.subscribe();

        roomJoinedHandler = captureHandler(RoomJoined.class);
        roomLeftHandler = captureHandler(RoomLeft.class);
        roundStartedHandler = captureHandler(RoundStarted.class);
        answerSubmittedHandler = captureHandler(AnswerSubmitted.class);
        roundEndedHandler = captureHandler(RoundEnded.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> EventHandler<T> captureHandler(Class<T> eventType) {
        ArgumentCaptor<EventHandler<T>> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(eventBus).subscribe(eq(eventType), captor.capture());
        return captor.getValue();
    }

    @Test
    void startsFirstRoundOnceTwoParticipantsHaveJoined() {
        String roomId = "room-1";
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-1"));
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-2"));

        ArgumentCaptor<StartRoundCommand> captor = ArgumentCaptor.forClass(StartRoundCommand.class);
        verify(commandBus).dispatch(captor.capture());
        assertThat(captor.getValue().roomId()).isEqualTo(roomId);
    }

    @Test
    void cancelsScheduledTimerWhenAllParticipantsAnswerBeforeTimeout() {
        String roomId = "room-1";
        String roundId = "round-1";
        when(roundRepository.findById(roundId)).thenReturn(Optional.empty());

        roomJoinedHandler.handle(new RoomJoined(roomId, "user-1"));
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-2"));
        roundStartedHandler.handle(new RoundStarted(roundId, roomId, "question", 1, 30));

        answerSubmittedHandler.handle(new AnswerSubmitted(roundId, roomId, "user-1", "a", "expected"));
        verify(timerPort, never()).cancelTimers(roomId);

        answerSubmittedHandler.handle(new AnswerSubmitted(roundId, roomId, "user-2", "a", "expected"));

        verify(timerPort).cancelTimers(roomId);
        verify(eventBus).publish(any(RoundEnded.class));
    }

    @Test
    void doesNotEndRoundTwiceWhenTimerFiresAfterEarlyEndAlreadyClaimedIt() {
        // Simulates the real TOCTOU race: ScheduledFuture.cancel(false) does not stop a
        // task that has already started, so the timer path can still reach "end this
        // round" after the early-end path already claimed and published RoundEnded.
        String roomId = "room-1";
        String roundId = "round-1";
        when(roundRepository.findById(roundId)).thenReturn(Optional.empty());

        roomJoinedHandler.handle(new RoomJoined(roomId, "user-1"));
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-2"));
        roundStartedHandler.handle(new RoundStarted(roundId, roomId, "question", 1, 30));

        answerSubmittedHandler.handle(new AnswerSubmitted(roundId, roomId, "user-1", "a", "expected"));
        answerSubmittedHandler.handle(new AnswerSubmitted(roundId, roomId, "user-2", "a", "expected"));

        // Early end already claimed roundId via roundEndGuard — a second claim attempt
        // (what ScheduledTimerAdapter's task would do) must fail.
        assertThat(roundEndGuard.tryClaim(roundId)).isFalse();
        verify(eventBus).publish(any(RoundEnded.class));
    }

    @Test
    void cancelsTimerWhenLastParticipantLeavesRoom() {
        String roomId = "room-1";
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-1"));

        roomLeftHandler.handle(new RoomLeft(roomId, "user-1"));

        verify(timerPort).cancelTimers(roomId);
    }

    @Test
    void endsGameAfterFinalRoundAndPublishesGameEnded() {
        String roomId = "room-1";
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-1"));
        roomJoinedHandler.handle(new RoomJoined(roomId, "user-2"));

        for (int i = 1; i <= 5; i++) {
            String roundId = "round-" + i;
            roundStartedHandler.handle(new RoundStarted(roundId, roomId, "question " + i, 1, 30));
            roundEndedHandler.handle(new RoundEnded(roundId, roomId,
                Map.of("user-1", 10), Map.of("user-1", "CORRECT")));
        }

        verify(eventBus).publish(any(GameEnded.class));
    }
}
