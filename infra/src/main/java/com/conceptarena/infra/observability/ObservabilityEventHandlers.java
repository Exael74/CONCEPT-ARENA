package com.conceptarena.infra.observability;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.concept.event.ConceptBankCreated;
import com.conceptarena.core.game.event.AnswerSubmitted;
import com.conceptarena.core.game.event.GameEnded;
import com.conceptarena.core.game.event.RoundEnded;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.room.event.RoomCreated;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.event.RoomLeft;
import com.conceptarena.core.user.event.UserLoggedIn;
import com.conceptarena.core.user.event.UserRegistered;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Structured log handler — populates MDC with domain context for every key event.
 * Fields emitted: timestamp, eventId, eventType, aggregateId, userId, roomId, roundId, result, latencyMs.
 */
@Component
public class ObservabilityEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityEventHandlers.class);
    private final EventBus eventBus;

    public ObservabilityEventHandlers(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(UserRegistered.class,   (EventHandler<UserRegistered>)   this::onUserRegistered);
        eventBus.subscribe(UserLoggedIn.class,     (EventHandler<UserLoggedIn>)     this::onUserLoggedIn);
        eventBus.subscribe(RoomCreated.class,      (EventHandler<RoomCreated>)      this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class,       (EventHandler<RoomJoined>)       this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class,         (EventHandler<RoomLeft>)         this::onRoomLeft);
        eventBus.subscribe(RoundStarted.class,     (EventHandler<RoundStarted>)     this::onRoundStarted);
        eventBus.subscribe(AnswerSubmitted.class,  (EventHandler<AnswerSubmitted>)  this::onAnswerSubmitted);
        eventBus.subscribe(RoundEnded.class,       (EventHandler<RoundEnded>)       this::onRoundEnded);
        eventBus.subscribe(GameEnded.class,        (EventHandler<GameEnded>)        this::onGameEnded);
        eventBus.subscribe(ConceptBankCreated.class, (EventHandler<ConceptBankCreated>) this::onConceptBankCreated);
    }

    private void onUserRegistered(UserRegistered event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "UserRegistered");
            MDC.put("userId", event.getAggregateId());
            log.info("User registered: email={}", event.getEmail().value());
        } finally { MDC.clear(); }
    }

    private void onUserLoggedIn(UserLoggedIn event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "UserLoggedIn");
            MDC.put("userId", event.getAggregateId());
            log.info("User logged in");
        } finally { MDC.clear(); }
    }

    private void onRoomCreated(RoomCreated event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomCreated");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getCreatorUserId());
            log.info("Room created: name={}, type={}", event.getName(), event.getType());
        } finally { MDC.clear(); }
    }

    private void onRoomJoined(RoomJoined event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomJoined");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getUserId());
            log.info("User joined room");
        } finally { MDC.clear(); }
    }

    private void onRoomLeft(RoomLeft event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomLeft");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getUserId());
            log.info("User left room");
        } finally { MDC.clear(); }
    }

    private void onRoundStarted(RoundStarted event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoundStarted");
            MDC.put("roundId", event.getAggregateId());
            MDC.put("roomId", event.getRoomId());
            log.info("Round started: difficulty={}, duration={}s",
                event.getDifficulty(), event.getDurationSeconds());
        } finally { MDC.clear(); }
    }

    private void onAnswerSubmitted(AnswerSubmitted event) {
        long latencyMs = Instant.now().toEpochMilli() - event.getOccurredOn().toEpochMilli();
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "AnswerSubmitted");
            MDC.put("roundId", event.getAggregateId());
            MDC.put("roomId", event.getRoomId());
            MDC.put("userId", event.getUserId());
            MDC.put("latencyMs", String.valueOf(latencyMs));
            log.info("Answer submitted");
        } finally { MDC.clear(); }
    }

    private void onRoundEnded(RoundEnded event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoundEnded");
            MDC.put("roundId", event.getAggregateId());
            MDC.put("roomId", event.getRoomId());
            log.info("Round ended: participants={}, scores={}",
                event.getScores().size(), event.getScores());
        } finally { MDC.clear(); }
    }

    private void onGameEnded(GameEnded event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "GameEnded");
            MDC.put("roomId", event.getRoomId());
            log.info("Game ended: finalScores={}", event.getFinalScores());
        } finally { MDC.clear(); }
    }

    private void onConceptBankCreated(ConceptBankCreated event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "ConceptBankCreated");
            log.info("ConceptBank created: name={}, subject={}", event.getName(), event.getSubject());
        } finally { MDC.clear(); }
    }
}
