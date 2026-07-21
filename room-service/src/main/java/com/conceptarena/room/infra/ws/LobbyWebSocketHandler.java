package com.conceptarena.room.infra.ws;

import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Raw (non-STOMP) /ws/lobby endpoint. Its session set and {@link #broadcast} are per-instance, so
 * they do NOT fan out across replicas — but the real lobby fan-out does not run through here: it
 * goes over the STOMP path (StompRoomNotificationAdapter -> /topic/lobby), which uses the shared
 * RabbitMQ STOMP relay under the docker profile (audit gap #7, see RoomStompConfig). broadcast() has
 * no callers today; this stays a thin raw-WS endpoint.
 */
@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class);
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Lobby WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Lobby WebSocket disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Lobby WS message from {}: {}", session.getId(), message.getPayload());
    }

    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                log.error("Error broadcasting to lobby session {}", session.getId(), e);
            }
        });
    }
}
