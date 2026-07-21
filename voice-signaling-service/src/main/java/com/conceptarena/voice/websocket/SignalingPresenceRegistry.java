package com.conceptarena.voice.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Tracks which userId is connected to which room's signaling channel, per this instance. Backs
 * both SignalingWebSocketHandler's relay lookups and SignalingStatusController's read endpoint.
 * In-memory / single-instance — see the pom.xml module description for why this isn't
 * Redis-externalized the way GameSaga was (audit gap #7): a live WebSocketSession only exists on
 * the one instance it's connected to, so sharing presence alone wouldn't let a different replica
 * actually relay to it.
 */
@Component
public class SignalingPresenceRegistry {

    private final Map<String, Map<String, WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

    public void join(String roomId, String userId, WebSocketSession session) {
        sessionsByRoom.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
    }

    public void leave(String roomId, String userId) {
        Map<String, WebSocketSession> room = sessionsByRoom.get(roomId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) {
                sessionsByRoom.remove(roomId);
            }
        }
    }

    /** Removes this session from whichever room it was in (used on connection close). */
    public void leaveAll(WebSocketSession session) {
        sessionsByRoom.values().forEach(room -> room.values().remove(session));
        sessionsByRoom.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public WebSocketSession find(String roomId, String userId) {
        Map<String, WebSocketSession> room = sessionsByRoom.get(roomId);
        return room == null ? null : room.get(userId);
    }

    public Set<String> participantsOf(String roomId) {
        Map<String, WebSocketSession> room = sessionsByRoom.get(roomId);
        return room == null ? Set.of() : Set.copyOf(room.keySet());
    }
}
