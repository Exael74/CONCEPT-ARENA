package com.conceptarena.game.infra.ws;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Split from the monolith's single StompNotificationAdapter — this copy only handles the
 * round/game events game-engine-service owns; room-service has its own copy for room events.
 */
@Component
public class StompGameNotificationAdapter {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messaging;

    public StompGameNotificationAdapter(EventBus eventBus, SimpMessagingTemplate messaging) {
        this.eventBus = eventBus;
        this.messaging = messaging;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) this::onRoundEnded);
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
    }

    private void onRoundStarted(RoundStarted event) {
        messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/round", Map.of(
            "type", "ROUND_STARTED",
            "roundId", event.getAggregateId(),
            "question", event.getConceptQuestion(),
            "difficulty", event.getDifficulty(),
            "durationSeconds", event.getDurationSeconds()
        ));
    }

    private void onRoundEnded(RoundEnded event) {
        messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/round", Map.of(
            "type", "ROUND_ENDED",
            "roundId", event.getAggregateId(),
            "scores", event.getScores()
        ));
    }

    private void onGameEnded(GameEnded event) {
        messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/round", Map.of(
            "type", "GAME_ENDED",
            "finalScores", event.getFinalScores()
        ));
    }
}
