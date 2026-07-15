package com.conceptarena.game.infra.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Covers the WebSocket authorization fix: the handler must trust only the userId the
 * handshake resolved from the JWT (WsJwtHandshakeInterceptor), never a userId claimed in the
 * message payload, and must enforce the per-user rate limit on "answer" messages.
 */
@ExtendWith(MockitoExtension.class)
class GameWebSocketHandlerTest {

    @Mock private CommandBus commandBus;
    @Mock private AnswerRateLimiter rateLimiter;
    @Mock private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GameWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GameWebSocketHandler(commandBus, objectMapper, rateLimiter);
    }

    private void authenticateSessionAs(String userId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WsJwtHandshakeInterceptor.WS_USER_ID_ATTRIBUTE, userId);
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    void dispatchesAnswerUsingHandshakeAuthenticatedUserIdNotThePayload() throws Exception {
        authenticateSessionAs("real-user");
        when(rateLimiter.allow("real-user")).thenReturn(true);

        String payload = objectMapper.writeValueAsString(Map.of(
            "type", "answer", "roomId", "room-1", "payload", "polymorphism"
        ));

        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<SubmitAnswerCommand> captor = ArgumentCaptor.forClass(SubmitAnswerCommand.class);
        verify(commandBus).dispatch(captor.capture());
        SubmitAnswerCommand command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.userId()).isEqualTo("real-user");
        org.assertj.core.api.Assertions.assertThat(command.roomId()).isEqualTo("room-1");
    }

    @Test
    void closesSessionWhenNoAuthenticatedUserIdIsPresent() throws Exception {
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.getId()).thenReturn("session-2");

        String payload = objectMapper.writeValueAsString(Map.of(
            "type", "answer", "roomId", "room-1", "payload", "polymorphism"
        ));

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(session).close(CloseStatus.NOT_ACCEPTABLE);
        verify(commandBus, never()).dispatch(any());
    }

    @Test
    void dropsAnswerWhenRateLimitExceeded() throws Exception {
        authenticateSessionAs("spammer");
        when(rateLimiter.allow("spammer")).thenReturn(true, true, true, false, false);

        String payload = objectMapper.writeValueAsString(Map.of(
            "type", "answer", "roomId", "room-1", "payload", "x"
        ));

        for (int i = 0; i < 5; i++) {
            handler.handleTextMessage(session, new TextMessage(payload));
        }

        verify(commandBus, times(3)).dispatch(any());
    }
}
