package com.conceptarena.game.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.game.app.GameSaga;
import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.infra.ws.AnswerRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
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
    @MockBean private RoundRepository roundRepository;

    private static Principal principal(String userId) {
        return () -> userId;
    }

    @Test
    void submitAnswerReturnsOk() throws Exception {
        when(rateLimiter.allow("user-1")).thenReturn(true);

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .principal(principal("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("polymorphism"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitAnswerReturnsBadRequestWhenNoActiveRound() throws Exception {
        when(rateLimiter.allow("user-1")).thenReturn(true);
        when(commandBus.dispatch(any())).thenThrow(new IllegalStateException("No active round for room: room-1"));

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .principal(principal("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("polymorphism"))))
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
                .principal(principal("spammer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("polymorphism"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.success").value(false));
    }

    /**
     * Closes audit gap #1: the userId used for scoring/anti-cheat must come from the
     * authenticated principal, never from the request body, or a caller authenticated as one
     * user could submit answers on behalf of another.
     */
    @Test
    void submitAnswerUsesAuthenticatedPrincipalNotRequestBody() throws Exception {
        when(rateLimiter.allow("real-user")).thenReturn(true);

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .principal(principal("real-user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("polymorphism"))))
            .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.conceptarena.game.domain.command.SubmitAnswerCommand> captor =
            org.mockito.ArgumentCaptor.forClass(com.conceptarena.game.domain.command.SubmitAnswerCommand.class);
        org.mockito.Mockito.verify(commandBus).dispatch(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("real-user", captor.getValue().userId());
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

    /**
     * REST fallback for a client that (re)connects mid-round: STOMP never replays past
     * broadcasts, so this is the only way such a client learns the current question.
     */
    @Test
    void getCurrentRoundReturnsActiveRoundData() throws Exception {
        Round round = new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofSeconds(30));
        round.start();
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.of(round));

        mockMvc.perform(get("/api/game/{roomId}/current-round", "room-1").principal(principal("user-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.question").value("What is polymorphism?"))
            .andExpect(jsonPath("$.data.difficulty").value(2))
            .andExpect(jsonPath("$.data.durationSeconds").value(30))
            .andExpect(jsonPath("$.data.alreadyAnswered").value(false));
    }

    @Test
    void getCurrentRoundMarksAlreadyAnsweredForTheCallingUser() throws Exception {
        Round round = new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofSeconds(30));
        round.start();
        round.submitAnswer("user-1", "polymorphism");
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.of(round));

        mockMvc.perform(get("/api/game/{roomId}/current-round", "room-1").principal(principal("user-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.alreadyAnswered").value(true));
    }

    @Test
    void getCurrentRoundReturnsNotFoundWhenNoActiveRound() throws Exception {
        when(roundRepository.findActiveRoundByRoomId("room-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/game/{roomId}/current-round", "room-1").principal(principal("user-1")))
            .andExpect(status().isNotFound());
    }
}
