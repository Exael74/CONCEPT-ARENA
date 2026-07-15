package com.conceptarena.room.infra.ws;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.app.bus.EventHandler;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import com.conceptarena.room.domain.event.RoomLeft;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Split from the monolith's single StompNotificationAdapter — this copy only handles the room
 * events room-service itself owns; game-engine-service has its own copy for round/game events.
 */
@Component
public class StompRoomNotificationAdapter {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messaging;

    public StompRoomNotificationAdapter(EventBus eventBus, SimpMessagingTemplate messaging) {
        this.eventBus = eventBus;
        this.messaging = messaging;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
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
}
