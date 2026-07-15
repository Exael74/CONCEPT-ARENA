package com.conceptarena.app.event;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.game.event.GameEnded;
import com.conceptarena.core.game.event.RoundEnded;
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
public class NotificationEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandlers.class);
    private final EventBus eventBus;

    public NotificationEventHandlers(EventBus eventBus) {
        this.eventBus = eventBus;
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
        log.info("NOTIFY RoomCreated: {} to /topic/lobby", event.getAggregateId());
    }

    private void onRoomJoined(RoomJoined event) {
        log.info("NOTIFY RoomJoined: user={} to /topic/rooms/{}/participants",
            event.getUserId(), event.getAggregateId());
    }

    private void onRoomLeft(RoomLeft event) {
        log.info("NOTIFY RoomLeft: user={} to /topic/rooms/{}/participants",
            event.getUserId(), event.getAggregateId());
    }

    private void onRoundStarted(RoundStarted event) {
        log.info("NOTIFY RoundStarted: to /topic/rooms/{}/round", event.getRoomId());
    }

    private void onRoundEnded(RoundEnded event) {
        log.info("NOTIFY RoundEnded: to /topic/rooms/{}/round", event.getRoomId());
    }

    private void onGameEnded(GameEnded event) {
        log.info("NOTIFY GameEnded: to /topic/rooms/{}/round", event.getAggregateId());
    }
}
