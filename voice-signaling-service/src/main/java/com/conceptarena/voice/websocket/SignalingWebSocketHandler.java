package com.conceptarena.voice.websocket;

import com.conceptarena.voice.dto.SignalingMessage;
import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Relays WebRTC offer/answer/ICE-candidate messages directly between two peers already
 * connected to the same room — never persisted, never queued: if the target peer isn't
 * currently connected, the message is dropped (matches how live signaling servers behave).
 *
 * fromUserId is always the handshake-authenticated userId (WsJwtHandshakeInterceptor), never
 * trusted from the message payload — same fix class as GameWebSocketHandler (audit gap #1).
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);

    private final SignalingPresenceRegistry presenceRegistry;
    private final ObjectMapper objectMapper;

    public SignalingWebSocketHandler(SignalingPresenceRegistry presenceRegistry, ObjectMapper objectMapper) {
        this.presenceRegistry = presenceRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get(WsJwtHandshakeInterceptor.WS_USER_ID_ATTRIBUTE);
        if (userId == null) {
            log.warn("Rejecting signaling WS message from session {} with no authenticated userId", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        SignalingMessage msg = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        switch (msg.type()) {
            case "join" -> presenceRegistry.join(msg.roomId(), userId, session);
            case "leave" -> presenceRegistry.leave(msg.roomId(), userId);
            case "offer", "answer", "ice-candidate" -> relay(msg, userId);
            default -> log.warn("Unknown signaling message type: {}", msg.type());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        presenceRegistry.leaveAll(session);
    }

    private void relay(SignalingMessage msg, String fromUserId) throws IOException {
        // A8: the sender must have joined this room's signaling channel before it can relay into it —
        // otherwise any authenticated user could push offers/ICE into a room they never joined. This
        // uses local presence (you must have sent "join" for msg.roomId() first); an authoritative
        // cross-check against room-service membership is future work (see ADR-013, voice).
        if (presenceRegistry.find(msg.roomId(), fromUserId) == null) {
            log.warn("Rejecting {} from user {} to room {}: sender is not a participant of that room",
                msg.type(), fromUserId, msg.roomId());
            return;
        }
        WebSocketSession target = presenceRegistry.find(msg.roomId(), msg.toUserId());
        if (target == null || !target.isOpen()) {
            log.debug("Dropping {} signal for room {}: target user {} not connected", msg.type(), msg.roomId(), msg.toUserId());
            return;
        }
        String relayed = objectMapper.writeValueAsString(new SignalingMessage(msg.type(), msg.roomId(), null, fromUserId, msg.payload()));
        target.sendMessage(new TextMessage(relayed));
    }
}
