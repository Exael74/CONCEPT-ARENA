package com.conceptarena.game.app;

import com.conceptarena.game.app.bus.CommandHandler;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort.ConceptSnapshot;
import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import com.conceptarena.game.app.readmodel.RoomReadModelPort.RoomSnapshot;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.event.RoundStarted;
import java.time.Duration;
import org.springframework.stereotype.Service;

/**
 * Rewritten against RoomReadModelPort/ConceptBankReadModelPort instead of the monolith's direct
 * RoomRepository/ConceptBankRepository — see ADR-004 for why (avoids a synchronous cross-service
 * call in the hottest path in the system).
 */
@Service
public class StartRoundCommandHandler implements CommandHandler<StartRoundCommand, Void> {

    private static final int ROUND_DURATION_SECONDS = 30;

    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final RoomReadModelPort roomReadModelPort;
    private final ConceptBankReadModelPort conceptBankReadModelPort;

    public StartRoundCommandHandler(EventBus eventBus, RoundRepository roundRepository,
                                     RoomReadModelPort roomReadModelPort, ConceptBankReadModelPort conceptBankReadModelPort) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.roomReadModelPort = roomReadModelPort;
        this.conceptBankReadModelPort = conceptBankReadModelPort;
    }

    @Override
    public Void handle(StartRoundCommand command) {
        RoomSnapshot room = roomReadModelPort.findByRoomId(command.roomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + command.roomId()));
        // Purely local bookkeeping — does NOT write back to room-service's Room.status (ADR-004).
        roomReadModelPort.markGameStarted(command.roomId());

        ConceptSnapshot concept = conceptBankReadModelPort.pickRandomConcept(room.conceptBankId());

        Round round = new Round(
            command.roomId(),
            concept.question(),
            concept.expectedAnswer(),
            concept.difficulty(),
            Duration.ofSeconds(ROUND_DURATION_SECONDS)
        );
        round.start();
        roundRepository.save(round);

        eventBus.publish(new RoundStarted(
            round.getId().value(), command.roomId(),
            concept.question(), concept.difficulty(), ROUND_DURATION_SECONDS
        ));

        return null;
    }
}
