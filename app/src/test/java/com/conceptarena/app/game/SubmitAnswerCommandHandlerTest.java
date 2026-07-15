package com.conceptarena.app.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.game.command.SubmitAnswerCommand;
import com.conceptarena.core.game.event.AnswerRejected;
import com.conceptarena.core.game.event.AnswerSubmitted;
import com.conceptarena.core.game.model.Answer;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomType;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitAnswerCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AnswerValidationPort answerValidationPort;

    private SubmitAnswerCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SubmitAnswerCommandHandler(eventBus, roundRepository, roomRepository, answerValidationPort);
    }

    private Round activeRound() {
        Round round = new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofSeconds(30));
        round.start();
        return round;
    }

    private Room roomWithParticipant(String userId) {
        Room room = Room.create("Room", RoomType.PUBLIC, null, "bank-1", 4);
        room.addParticipant(userId);
        return room;
    }

    @Test
    void marksAnswerCorrectWhenValidationPortApproves() {
        Round round = activeRound();
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(roomWithParticipant("user-1")));
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
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(roomWithParticipant("user-1")));
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
        verifyNoInteractions(roundRepository, roomRepository, answerValidationPort);
        verify(eventBus).publish(any(AnswerRejected.class));
    }

    @Test
    void rejectsAnswerLongerThanMaxLength() {
        String tooLong = "a".repeat(501);
        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", tooLong)))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(roundRepository, roomRepository, answerValidationPort);
    }

    @Test
    void rejectsWhenNoActiveRoundForRoom() {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(roomWithParticipant("user-1")));
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWhenUserIsNotAParticipantOfTheRoom() {
        when(roomRepository.findById("room-1")).thenReturn(Optional.of(roomWithParticipant("someone-else")));

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "attacker", "polymorphism")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not a participant");
        verifyNoInteractions(roundRepository, answerValidationPort);
    }

    @Test
    void rejectsWhenRoomDoesNotExist() {
        when(roomRepository.findById("room-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new SubmitAnswerCommand("room-1", "user-1", "polymorphism")))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(roundRepository, answerValidationPort);
    }
}
