package com.conceptarena.game.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.game.app.GameSaga;
import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.infra.ws.AnswerRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private GameSaga gameSaga;
    @MockBean private AnswerRateLimiter rateLimiter;
    @MockBean private MeterRegistry meterRegistry;

    @Test
    void submitAnswerReturnsOk() throws Exception {
        when(rateLimiter.allow("user-1")).thenReturn(true);

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("user-1", "polymorphism"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitAnswerReturnsBadRequestWhenNoActiveRound() throws Exception {
        when(rateLimiter.allow("user-1")).thenReturn(true);
        when(commandBus.dispatch(any())).thenThrow(new IllegalStateException("No active round for room: room-1"));

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("user-1", "polymorphism"))))
            .andExpect(status().isBadRequest());
    }

    /**
     * Closes audit gap #3: the monolith enforced the 3/sec answer rate limit only on the WS
     * path, leaving this REST endpoint free to be spammed. Must return 429, and must never
     * reach commandBus.dispatch when the limiter rejects.
     */
    @Test
    void submitAnswerReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
        when(rateLimiter.allow("spammer")).thenReturn(false);

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("spammer", "polymorphism"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRankingReturnsScoresForActiveGame() throws Exception {
        when(gameSaga.getScores("room-1")).thenReturn(Map.of("user-1", 30));

        mockMvc.perform(get("/api/game/{roomId}/ranking", "room-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data['user-1']").value(30));
    }

    @Test
    void getRankingReturnsNotFoundWhenNoGameActive() throws Exception {
        when(gameSaga.getScores("room-1")).thenReturn(null);

        mockMvc.perform(get("/api/game/{roomId}/ranking", "room-1"))
            .andExpect(status().isNotFound());
    }
}
