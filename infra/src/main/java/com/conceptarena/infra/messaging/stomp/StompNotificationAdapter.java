package com.conceptarena.infra.messaging.stomp;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.game.event.GameEnded;
import com.conceptarena.core.game.event.RoundEnded;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.room.event.RoomCreated;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.event.RoomLeft;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompNotificationAdapter {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messaging;

    public StompNotificationAdapter(EventBus eventBus, SimpMessagingTemplate messaging) {
        this.eventBus = eventBus;
        this.messaging = messaging;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) this::onRoundEnded);
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
    }

    private void onRoomCreated(RoomCreated event) {
        messaging.convertAndSend("/topic/lobby", Map.of(
            "type", "ROOM_CREATED",
            "roomId", event.getAggregateId(),
            "name", event.getName(),
            "roomType", event.getType().name()
        ));
    }

    private void onRoomJoined(RoomJoined event) {
        messaging.convertAndSend("/topic/rooms/" + event.getAggregateId() + "/participants", Map.of(
            "type", "USER_JOINED",
            "userId", event.getUserId()
        ));
    }

    private void onRoomLeft(RoomLeft event) {
        messaging.convertAndSend("/topic/rooms/" + event.getAggregateId() + "/participants", Map.of(
            "type", "USER_LEFT",
            "userId", event.getUserId()
        ));
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

