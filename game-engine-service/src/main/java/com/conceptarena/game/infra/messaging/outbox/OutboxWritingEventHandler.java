package com.conceptarena.game.infra.messaging.outbox;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * See docs/event-contracts.md. AnswerRejected is deliberately NOT published here — it stays
 * local-only (consumed only by MicrometerMetricsAdapter in this same service); no other service
 * needs it, and it's a same-request rejection, not state other services need to react to.
 */
@Component
public class OutboxWritingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxWritingEventHandler.class);
    private static final String EXCHANGE = "conceptarena.game.events";

    private final EventBus eventBus;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxWritingEventHandler(EventBus eventBus, OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper) {
        this.eventBus = eventBus;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) this::onAnswerSubmitted);
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) this::onRoundEnded);
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
    }

    private void onRoundStarted(RoundStarted event) {
        write(event.getAggregateId(), "RoundStarted", "game.round-started", event);
    }

    private void onAnswerSubmitted(AnswerSubmitted event) {
        write(event.getAggregateId(), "AnswerSubmitted", "game.answer-submitted", event);
    }

    private void onRoundEnded(RoundEnded event) {
        write(event.getAggregateId(), "RoundEnded", "game.round-ended", event);
    }

    private void onGameEnded(GameEnded event) {
        write(event.getAggregateId(), "GameEnded", "game.game-ended", event);
    }

    private void write(String aggregateId, String eventType, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent row = new OutboxEvent(
                UUID.randomUUID().toString(), aggregateId, eventType, EXCHANGE, routingKey, json, Instant.now());
            outboxEventRepository.save(row);
        } catch (Exception e) {
            log.error("Failed to write outbox row for {} (aggregateId={}): {}", eventType, aggregateId, e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize event for outbox: " + eventType, e);
        }
    }
}
