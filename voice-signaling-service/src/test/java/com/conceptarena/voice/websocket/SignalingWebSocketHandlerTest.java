package com.conceptarena.voice.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import com.conceptarena.voice.dto.SignalingMessage;
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
 * Covers the same anti-spoofing property GameWebSocketHandlerTest covers for game-engine-service
 * (audit gap #1's class of bug): fromUserId must always be the handshake-authenticated userId,
 * never a client-claimed value, and only a currently-connected target session receives a relay.
 */
@ExtendWith(MockitoExtension.class)
class SignalingWebSocketHandlerTest {

    @Mock private WebSocketSession fromSession;
    @Mock private WebSocketSession toSession;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SignalingPresenceRegistry presenceRegistry;
    private SignalingWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        presenceRegistry = new SignalingPresenceRegistry();
        handler = new SignalingWebSocketHandler(presenceRegistry, objectMapper);
    }

    private void authenticateSessionAs(WebSocketSession session, String userId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WsJwtHandshakeInterceptor.WS_USER_ID_ATTRIBUTE, userId);
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    void relaysOfferToConnectedTargetUsingHandshakeAuthenticatedFromUserId() throws Exception {
        authenticateSessionAs(fromSession, "caller");
        when(toSession.isOpen()).thenReturn(true);
        presenceRegistry.join("room-1", "caller", fromSession);   // A8: sender must have joined the room
        presenceRegistry.join("room-1", "callee", toSession);

        String payload = objectMapper.writeValueAsString(
            new SignalingMessage("offer", "room-1", "callee", "someone-claiming-to-be-someone-else", "sdp-offer"));
        handler.handleTextMessage(fromSession, new TextMessage(payload));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(toSession).sendMessage(captor.capture());
        SignalingMessage relayed = objectMapper.readValue(captor.getValue().getPayload(), SignalingMessage.class);
        org.assertj.core.api.Assertions.assertThat(relayed.fromUserId()).isEqualTo("caller");
        org.assertj.core.api.Assertions.assertThat(relayed.payload()).isEqualTo("sdp-offer");
    }

    @Test
    void dropsSignalWhenTargetUserIsNotConnected() throws Exception {
        authenticateSessionAs(fromSession, "caller");

        String payload = objectMapper.writeValueAsString(
            new SignalingMessage("offer", "room-1", "nobody-home", null, "sdp-offer"));
        handler.handleTextMessage(fromSession, new TextMessage(payload));

        verify(toSession, never()).sendMessage(any());
    }

    @Test
    void dropsSignalWhenSenderHasNotJoinedTheRoom() throws Exception {
        // A8: an authenticated user who never joined room-1 must not be able to relay into it, even
        // if the target is connected there.
        authenticateSessionAs(fromSession, "outsider");
        presenceRegistry.join("room-1", "callee", toSession);

        String payload = objectMapper.writeValueAsString(
            new SignalingMessage("offer", "room-1", "callee", null, "sdp-offer"));
        handler.handleTextMessage(fromSession, new TextMessage(payload));

        verify(toSession, never()).sendMessage(any());
    }

    @Test
    void closesSessionWhenNoAuthenticatedUserIdIsPresent() throws Exception {
        when(fromSession.getAttributes()).thenReturn(new HashMap<>());
        when(fromSession.getId()).thenReturn("session-1");

        String payload = objectMapper.writeValueAsString(new SignalingMessage("join", "room-1", null, null, null));
        handler.handleTextMessage(fromSession, new TextMessage(payload));

        verify(fromSession).close(CloseStatus.NOT_ACCEPTABLE);
    }
}
