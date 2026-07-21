package com.conceptarena.voice.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

/** C6: unit-tests for the presence registry (join/leave/find/participants and cleanup). */
class SignalingPresenceRegistryTest {

    private final SignalingPresenceRegistry registry = new SignalingPresenceRegistry();

    @Test
    void joinThenFindReturnsTheSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        registry.join("room-1", "user-1", session);

        assertThat(registry.find("room-1", "user-1")).isSameAs(session);
        assertThat(registry.participantsOf("room-1")).containsExactly("user-1");
    }

    @Test
    void leaveRemovesTheUserAndPrunesEmptyRoom() {
        registry.join("room-1", "user-1", mock(WebSocketSession.class));
        registry.leave("room-1", "user-1");

        assertThat(registry.find("room-1", "user-1")).isNull();
        assertThat(registry.participantsOf("room-1")).isEmpty();
    }

    @Test
    void leaveAllRemovesTheSessionFromEveryRoom() {
        WebSocketSession session = mock(WebSocketSession.class);
        registry.join("room-1", "user-1", session);
        registry.join("room-2", "user-1", session);

        registry.leaveAll(session);

        assertThat(registry.find("room-1", "user-1")).isNull();
        assertThat(registry.find("room-2", "user-1")).isNull();
    }

    @Test
    void participantsOfUnknownRoomIsEmpty() {
        assertThat(registry.participantsOf("nope")).isEmpty();
    }
}
