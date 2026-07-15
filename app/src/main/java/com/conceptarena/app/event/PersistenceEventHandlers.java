package com.conceptarena.app.event;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.concept.event.ConceptBankCreated;
import com.conceptarena.core.game.event.AnswerSubmitted;
import com.conceptarena.core.game.event.GameEnded;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.room.event.RoomCreated;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.event.RoomLeft;
import com.conceptarena.core.user.event.UserRegistered;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PersistenceEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(PersistenceEventHandlers.class);
    private final EventBus eventBus;

    public PersistenceEventHandlers(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(UserRegistered.class, (EventHandler<UserRegistered>) this::onUserRegistered);
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) this::onAnswerSubmitted);
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
        eventBus.subscribe(ConceptBankCreated.class, (EventHandler<ConceptBankCreated>) this::onConceptBankCreated);
    }

    private void onUserRegistered(UserRegistered event) {
        log.info("PERSIST UserRegistered: aggregateId={}, email={}", event.getAggregateId(), event.getEmail());
    }

    private void onRoomCreated(RoomCreated event) {
        log.info("PERSIST RoomCreated: aggregateId={}, name={}", event.getAggregateId(), event.getName());
    }

    private void onRoomJoined(RoomJoined event) {
        log.info("PERSIST RoomJoined: roomId={}, userId={}", event.getAggregateId(), event.getUserId());
    }

    private void onRoomLeft(RoomLeft event) {
        log.info("PERSIST RoomLeft: roomId={}, userId={}", event.getAggregateId(), event.getUserId());
    }

    private void onRoundStarted(RoundStarted event) {
        log.info("PERSIST RoundStarted: roundId={}, roomId={}", event.getAggregateId(), event.getRoomId());
    }

    private void onAnswerSubmitted(AnswerSubmitted event) {
        log.info("PERSIST AnswerSubmitted: roundId={}, userId={}", event.getAggregateId(), event.getUserId());
    }

    private void onGameEnded(GameEnded event) {
        log.info("PERSIST GameEnded: roomId={}", event.getRoomId());
    }

    private void onConceptBankCreated(ConceptBankCreated event) {
        log.info("PERSIST ConceptBankCreated: bankId={}, name={}", event.getAggregateId(), event.getName());
    }
}
