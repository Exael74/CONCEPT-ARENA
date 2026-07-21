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
import com.conceptarena.game.domain.error.GameAlreadyInProgressException;
import com.conceptarena.game.domain.error.NotRoomOwnerException;
import com.conceptarena.game.domain.event.RoundStarted;
import java.time.Duration;
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
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-1"))
            .thenReturn(Optional.of(new ConceptSnapshot("What is OOP?", "object oriented programming", 3)));

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
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-empty", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-empty"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("room-1", "system")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ownerCanStartTheirOwnRoom() {
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-1"))
            .thenReturn(Optional.of(new ConceptSnapshot("What is OOP?", "object oriented programming", 3)));

        handler.handle(new StartRoundCommand("room-1", "creator-1"));

        verify(roundRepository).save(any(Round.class));
    }

    @Test
    void rejectsWhenNonCreatorTriesToStart() {
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("room-1", "someone-else")))
            .isInstanceOf(NotRoomOwnerException.class);
        verify(roundRepository, org.mockito.Mockito.never()).save(any(Round.class));
    }

    @Test
    void rejectsExplicitStartWhenARoundIsAlreadyActive() {
        // Owner double-click / start-while-running: must not create a second ACTIVE round.
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(roundRepository.findActiveRoundByRoomId("room-1"))
            .thenReturn(Optional.of(new Round("room-1", "q", "a", 1, Duration.ofSeconds(30))));

        assertThatThrownBy(() -> handler.handle(new StartRoundCommand("room-1", "creator-1")))
            .isInstanceOf(GameAlreadyInProgressException.class);
        verify(roundRepository, org.mockito.Mockito.never()).save(any(Round.class));
    }

    @Test
    void systemTriggeredStartSkipsTheAlreadyActiveCheck() {
        // The next-round progression (system-triggered) always runs after the prior round ENDED,
        // so it must not be blocked by the owner-only "already in progress" guard.
        RoomSnapshot room = new RoomSnapshot("room-1", "creator-1", "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-1"))
            .thenReturn(Optional.of(new ConceptSnapshot("What is OOP?", "object oriented programming", 3)));

        handler.handle(new StartRoundCommand("room-1", StartRoundCommand.SYSTEM_TRIGGERED));

        verify(roundRepository).save(any(Round.class));
        verify(roundRepository, org.mockito.Mockito.never()).findActiveRoundByRoomId(any());
    }

    @Test
    void failsOpenWhenRoomHasNoRecordedCreator() {
        // Legacy room read-model row predating the creatorUserId column — must not permanently lock everyone out.
        RoomSnapshot room = new RoomSnapshot("room-1", null, "bank-1", 4, false);
        when(roomReadModelPort.findByRoomId("room-1")).thenReturn(Optional.of(room));
        when(conceptBankReadModelPort.pickRandomConcept("bank-1"))
            .thenReturn(Optional.of(new ConceptSnapshot("What is OOP?", "object oriented programming", 3)));

        handler.handle(new StartRoundCommand("room-1", "anyone"));

        verify(roundRepository).save(any(Round.class));
    }
}
