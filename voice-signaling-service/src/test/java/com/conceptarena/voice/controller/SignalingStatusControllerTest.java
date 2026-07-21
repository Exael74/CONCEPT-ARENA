package com.conceptarena.voice.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.voice.websocket.SignalingPresenceRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * C4: previously voice had no REST test. Covers the participants endpoint + the error path.
 * standaloneSetup wires the controller together with GlobalExceptionHandler deterministically, so
 * the error path genuinely exercises the @RestControllerAdvice (a real MeterRegistry, since the
 * handler builds a Counter on it).
 */
class SignalingStatusControllerTest {

    private final SignalingPresenceRegistry presenceRegistry = mock(SignalingPresenceRegistry.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SignalingStatusController(presenceRegistry))
            .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
            .build();
    }

    @Test
    void returnsConnectedParticipantsForRoom() throws Exception {
        when(presenceRegistry.participantsOf("room-1")).thenReturn(Set.of("user-1", "user-2"));

        mockMvc.perform(get("/api/signaling/{roomId}/participants", "room-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void returnsEmptySetForUnknownRoom() throws Exception {
        when(presenceRegistry.participantsOf("nope")).thenReturn(Set.of());

        mockMvc.perform(get("/api/signaling/{roomId}/participants", "nope"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void unexpectedErrorIsHandledAs500ByGlobalExceptionHandler() throws Exception {
        // C4: a thrown RuntimeException must surface as a clean 500 ApiResponse via the advice.
        when(presenceRegistry.participantsOf("boom")).thenThrow(new RuntimeException("kaboom"));

        mockMvc.perform(get("/api/signaling/{roomId}/participants", "boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false));
    }
}
