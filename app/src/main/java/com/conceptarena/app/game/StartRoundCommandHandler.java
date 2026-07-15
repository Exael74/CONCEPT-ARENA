package com.conceptarena.app.game;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.concept.ConceptBankRepository;
import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.concept.model.Concept;
import com.conceptarena.core.concept.model.ConceptBank;
import com.conceptarena.core.game.command.StartRoundCommand;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.core.room.model.Room;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class StartRoundCommandHandler implements CommandHandler<StartRoundCommand, Void> {

    private static final int ROUND_DURATION_SECONDS = 30;

    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final RoomRepository roomRepository;
    private final ConceptBankRepository conceptBankRepository;

    public StartRoundCommandHandler(EventBus eventBus, RoundRepository roundRepository,
                                    RoomRepository roomRepository, ConceptBankRepository conceptBankRepository) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.roomRepository = roomRepository;
        this.conceptBankRepository = conceptBankRepository;
    }

    @Override
    public Void handle(StartRoundCommand command) {
        Room room = roomRepository.findById(command.roomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + command.roomId()));
        room.startGame();
        roomRepository.save(room);

        Concept concept = pickConcept(room.getConceptBankId());

        Round round = new Round(
            command.roomId(),
            concept.getQuestion(),
            concept.getExpectedAnswer(),
            concept.getDifficulty(),
            Duration.ofSeconds(ROUND_DURATION_SECONDS)
        );
        round.start();
        roundRepository.save(round);

        eventBus.publish(new RoundStarted(
            round.getId().value(), command.roomId(),
            concept.getQuestion(), concept.getDifficulty(), ROUND_DURATION_SECONDS
        ));

        return null;
    }

    private Concept pickConcept(String conceptBankId) {
        ConceptBank bank = conceptBankRepository.findById(conceptBankId)
            .orElseThrow(() -> new IllegalStateException("ConceptBank not found: " + conceptBankId));
        List<Concept> concepts = bank.getConcepts();
        if (concepts.isEmpty()) {
            throw new IllegalStateException("ConceptBank has no concepts: " + conceptBankId);
        }
        return concepts.get(ThreadLocalRandom.current().nextInt(concepts.size()));
    }
}

