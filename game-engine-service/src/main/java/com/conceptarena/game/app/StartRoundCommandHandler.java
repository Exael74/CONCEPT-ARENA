package com.conceptarena.game.app;

import com.conceptarena.game.app.bus.CommandHandler;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort;
import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort.ConceptSnapshot;
import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import com.conceptarena.game.app.readmodel.RoomReadModelPort.RoomSnapshot;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.error.NotRoomOwnerException;
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

        // GameSaga's own auto-starts (2 players joined / next round) use SYSTEM_TRIGGERED and skip
        // this check. A null creatorUserId means the room predates this field (no backfill possible
        // for room-service's Redis-held state) — fails open rather than permanently locking it.
        boolean isSystem = StartRoundCommand.SYSTEM_TRIGGERED.equals(command.triggeredByUserId());
        if (!isSystem && room.creatorUserId() != null && !room.creatorUserId().equals(command.triggeredByUserId())) {
            throw new NotRoomOwnerException(command.roomId());
        }

        ConceptSnapshot concept = conceptBankReadModelPort.pickRandomConcept(room.conceptBankId())
            .orElseThrow(() -> new IllegalStateException(
                "ConceptBank has no concepts (or is unknown): " + room.conceptBankId()));

        Round round = new Round(
            command.roomId(),
            concept.question(),
            concept.expectedAnswer(),
            concept.difficulty(),
            Duration.ofSeconds(ROUND_DURATION_SECONDS)
        );
        round.start();
        roundRepository.save(round);
        // Only marked once the round actually exists — previously ran before the concept lookup,
        // so a failed start (e.g. concept bank not yet populated) left gameStarted=true with no
        // round ever created.
        roomReadModelPort.markGameStarted(command.roomId());

        eventBus.publish(new RoundStarted(
            round.getId().value(), command.roomId(),
            concept.question(), concept.difficulty(), ROUND_DURATION_SECONDS
        ));

        return null;
    }
}
