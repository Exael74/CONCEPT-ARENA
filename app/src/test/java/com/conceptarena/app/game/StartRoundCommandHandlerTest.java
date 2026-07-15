package com.conceptarena.app.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.concept.ConceptBankRepository;
import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.concept.model.Concept;
import com.conceptarena.core.concept.model.ConceptBank;
import com.conceptarena.core.game.command.StartRoundCommand;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.core.game.model.RoundStatus;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
import com.conceptarena.core.room.model.RoomType;
import com.conceptarena.core.shared.valueobject.EntityId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartRoundCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ConceptBankRepository conceptBankRepository;

    private StartRoundCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StartRoundCommandHandler(eventBus, roundRepository, roomRepository, conceptBankRepository);
    }

    @Test
    void startsRoundWithConceptFromRoomsBankAndPublishesEvent() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        // restore() bypasses the >=5-concept creation invariant so the test can pin down
        // exactly which concept gets picked, without it affecting the assertions below.
        ConceptBank bank = ConceptBank.restore(EntityId.generate(), "Bank", "Subject", List.of(
            new Concept("What is OOP?", "object oriented programming", 3)));
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));
        when(conceptBankRepository.findById("bank-1")).thenReturn(Optional.of(bank));

        handler.handle(new StartRoundCommand(room.getId().value(), "system"));

        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_GAME);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(roundCaptor.capture());
        Round savedRound = roundCaptor.getValue();
        assertThat(savedRound.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThat(savedRound.getConceptQuestion()).isEqualTo("What is OOP?");
        assertThat(savedRound.getExpectedAnswer()).isEqualTo("object oriented programming");

        verify(eventBus).publish(any(RoundStarted.class));
    }

    @Test
    void rejectsWhenRoomDoesNotExist() {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("missing", "system")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWhenConceptBankHasNoConcepts() {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-empty", 4);
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));
        when(conceptBankRepository.findById("bank-empty")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand(room.getId().value(), "system")))
            .isInstanceOf(IllegalStateException.class);
    }
}
