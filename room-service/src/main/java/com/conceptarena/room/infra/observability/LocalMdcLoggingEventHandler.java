package com.conceptarena.room.infra.observability;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.app.bus.EventHandler;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import com.conceptarena.room.domain.event.RoomLeft;
import jakarta.annotation.PostConstruct;
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
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) this::onRoomCreated);
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
    }

    private void onRoomCreated(RoomCreated event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomCreated");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getCreatorUserId());
            log.info("Room created: name={}, type={}", event.getName(), event.getType());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roomId");
            MDC.remove("userId");
        }
    }

    private void onRoomJoined(RoomJoined event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomJoined");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getUserId());
            log.info("User joined room");
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roomId");
            MDC.remove("userId");
        }
    }

    private void onRoomLeft(RoomLeft event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "RoomLeft");
            MDC.put("roomId", event.getAggregateId());
            MDC.put("userId", event.getUserId());
            log.info("User left room");
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("roomId");
            MDC.remove("userId");
        }
    }
}
