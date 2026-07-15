package com.conceptarena.game.infra.observability;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LocalMdcLoggingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LocalMdcLoggingEventHandler.class);
    private final EventBus eventBus;

    public LocalMdcLoggingEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) this::onAnswerSubmitted);
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) this::onRoundEnded);
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
    }

    private void onRoundStarted(RoundStarted event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoundStarted");
            MDC.put("roundId", event.getAggregateId());
            MDC.put("roomId", event.getRoomId());
            log.info("Round started: difficulty={}, duration={}s", event.getDifficulty(), event.getDurationSeconds());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roundId");
            MDC.remove("roomId");
        }
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
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roundId");
            MDC.remove("roomId");
            MDC.remove("userId");
            MDC.remove("latencyMs");
        }
    }

    private void onRoundEnded(RoundEnded event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoundEnded");
            MDC.put("roundId", event.getAggregateId());
            MDC.put("roomId", event.getRoomId());
            log.info("Round ended: participants={}, scores={}", event.getScores().size(), event.getScores());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roundId");
            MDC.remove("roomId");
        }
    }

    private void onGameEnded(GameEnded event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "GameEnded");
            MDC.put("roomId", event.getRoomId());
            log.info("Game ended: finalScores={}", event.getFinalScores());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roomId");
        }
    }
}
