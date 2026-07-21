package com.conceptarena.game.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.game.domain.event.AnswerRejected;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Rewritten (per ADR-004) to mock RoomReadModelPort.isParticipant instead of RoomRepository. */
@ExtendWith(MockitoExtension.class)
class SubmitAnswerCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private RoomReadModelPort roomReadModelPort;
    @Mock private AnswerValidationPort answerValidationPort;

    private SubmitAnswerCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SubmitAnswerCommandHandler(eventBus, roundRepository, roomReadModelPort, answerValidationPort);
    }

    private Round activeRound() {
        Round round = new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofSeconds(30));
        round.start();
        return round;
    }

    @Test
    void marksAnswerCorrectWhenValidationPortApproves() {
        Round round = activeRound();
        when(roomReadModelPort.isParticipant("room-1", "user-1")).thenReturn(true);
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.of(round));
        when(answerValidationPort.isCorrect("polymorphism", "polymorphism")).thenReturn(true);

        handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism"));

        Answer answer = round.getAnswers().get("user-1");
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.CORRECT);
        verify(roundRepository).save(round);
        verify(eventBus).publish(any(AnswerSubmitted.class));
    }

    @Test
    void marksAnswerIncorrectWhenValidationPortRejects() {
        Round round = activeRound();
        when(roomReadModelPort.isParticipant("room-1", "user-1")).thenReturn(true);
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.of(round));
        when(answerValidationPort.isCorrect("wrong answer", "polymorphism")).thenReturn(false);

        handler.handle(new SubmitAnswerCommand("room-1", "user-1", "wrong answer"));

        Answer answer = round.getAnswers().get("user-1");
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.INCORRECT);
    }

    @Test
    void rejectsBlankAnswerBeforeTouchingTheRound() {
        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "   ")))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(roundRepository, roomReadModelPort, answerValidationPort);
        verify(eventBus).publish(any(AnswerRejected.class));
    }

    @Test
    void rejectsAnswerLongerThanMaxLength() {
        String tooLong = "a".repeat(501);
        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", tooLong)))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(roundRepository, roomReadModelPort, answerValidationPort);
    }

    @Test
    void rejectsWhenNoActiveRoundForRoom() {
        when(roomReadModelPort.isParticipant("room-1", "user-1")).thenReturn(true);
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWhenUserIsNotAParticipantOfTheRoom() {
        when(roomReadModelPort.isParticipant("room-1", "attacker")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "attacker", "polymorphism")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not a participant");
        verifyNoInteractions(roundRepository, answerValidationPort);
    }

    @Test
    void rejectsCleanlyInsteadOf500WhenRoundWasConcurrentlyTransitioned() {
        // Reproduces the production bug (2026-07-21): this Round was loaded here, but by save()
        // time it had already been ended/transitioned by a concurrent early-end or timer expiry —
        // the @Version bump on the now-ended row makes this save stale. Must surface as a clean
        // 400-worthy rejection, not let the raw ObjectOptimisticLockingFailureException (previously
        // an uncaught IncorrectResultSizeDataAccessException from two ACTIVE rows existing at once)
        // reach GameController as a 500.
        Round round = activeRound();
        when(roomReadModelPort.isParticipant("room-1", "user-1")).thenReturn(true);
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.of(round));
        when(answerValidationPort.isCorrect("polymorphism", "polymorphism")).thenReturn(true);
        when(roundRepository.save(round)).thenThrow(new ObjectOptimisticLockingFailureException(Round.class, "round-1"));

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism")))
            .isInstanceOf(IllegalStateException.class);

        verify(eventBus).publish(any(AnswerRejected.class));
        verify(eventBus, org.mockito.Mockito.never()).publish(any(AnswerSubmitted.class));
    }

    @Test
    void rejectsWhenRoomUnknownToReadModel() {
        when(roomReadModelPort.isParticipant("room-1", "user-1")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism")))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(roundRepository, answerValidationPort);
    }
}
