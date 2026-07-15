package com.conceptarena.game.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort.ConceptSnapshot;
import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import com.conceptarena.game.app.readmodel.RoomReadModelPort.RoomSnapshot;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.RoundStatus;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.event.RoundStarted;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Rewritten (per ADR-004) to mock RoomReadModelPort/ConceptBankReadModelPort instead of the
 * monolith's RoomRepository/ConceptBankRepository — the coupling fix this phase exists for.
 */
@ExtendWith(MockitoExtension.class)
class StartRoundCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private RoundRepository roundRepository;
    @Mock private RoomReadModelPort roomReadModelPort;
    @Mock private ConceptBankReadModelPort conceptBankReadModelPort;

    private StartRoundCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StartRoundCommandHandler(eventBus, roundRepository, roomReadModelPort, conceptBankReadModelPort);
    }

    @Test
    void startsRoundWithConceptFromRoomsBankAndPublishesEvent() {
        RoomSnapshot room = new RoomSnapshot("room-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-1"))
            .thenReturn(new ConceptSnapshot("What is OOP?", "object oriented programming", 3));

        handler.handle(new StartRoundCommand("room-1", "system"));

        verify(roomReadModelPort).markGameStarted("room-1");

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(roundCaptor.capture());
        Round savedRound = roundCaptor.getValue();
        assertThat(savedRound.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThat(savedRound.getConceptQuestion()).isEqualTo("What is OOP?");
        assertThat(savedRound.getExpectedAnswer()).isEqualTo("object oriented programming");

        verify(eventBus).publish(any(RoundStarted.class));
    }

    @Test
    void rejectsWhenRoomDoesNotExistInReadModel() {
        when(roomReadModelPort.findByRoomId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("missing", "system")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWhenConceptBankHasNoConcepts() {
        RoomSnapshot room = new RoomSnapshot("room-1", "bank-empty", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-empty"))
            .thenThrow(new IllegalStateException("ConceptBank has no concepts (or is unknown): bank-empty"));

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("room-1", "system")))
            .isInstanceOf(IllegalStateException.class);
    }
}
