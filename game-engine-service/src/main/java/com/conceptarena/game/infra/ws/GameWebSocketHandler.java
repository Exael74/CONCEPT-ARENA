package com.conceptarena.game.infra.ws;

import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    // A5: reject oversized frames before parsing. Answers are short (REST caps at 500 chars); 4KB is
    // generous headroom while stopping a client from streaming a huge payload to exhaust memory.
    private static final int MAX_PAYLOAD_BYTES = 4096;
    private final CommandBus commandBus;
    private final ObjectMapper objectMapper;
    private final AnswerRateLimiter rateLimiter;

    public GameWebSocketHandler(CommandBus commandBus, ObjectMapper objectMapper, AnswerRateLimiter rateLimiter) {
        this.commandBus = commandBus;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // A5: drop oversized payloads before doing any work with them.
        if (message.getPayloadLength() > MAX_PAYLOAD_BYTES) {
            log.warn("Rejecting oversized game WS message from session {} ({} bytes > {})",
                session.getId(), message.getPayloadLength(), MAX_PAYLOAD_BYTES);
            return;
        }
        // The handshake (WsJwtHandshakeInterceptor) already authenticated this session and
        // stashed the real userId here — the client's own claimed userId in the payload,
        // if any, is never trusted for authorization decisions.
        String userId = (String) session.getAttributes().get(WsJwtHandshakeInterceptor.WS_USER_ID_ATTRIBUTE);
        if (userId == null) {
            log.warn("Rejecting game WS message from session {} with no authenticated userId", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        GameMessage msg = objectMapper.readValue(message.getPayload(), GameMessage.class);
        switch (msg.type()) {
            case "answer" -> {
                if (!rateLimiter.allow(userId)) {
                    log.warn("Rate limit exceeded for user {} on room {} — dropping answer", userId, msg.roomId());
                    return;
                }
                commandBus.dispatch(new SubmitAnswerCommand(msg.roomId(), userId, msg.payload()));
            }
            default -> log.warn("Unknown game message type: {}", msg.type());
        }
    }

    private record GameMessage(String type, String roomId, String payload) {}
}
