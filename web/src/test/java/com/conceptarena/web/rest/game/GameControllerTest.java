package com.conceptarena.web.rest.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.app.game.GameSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private GameSaga gameSaga;

    @Test
    void submitAnswerReturnsOk() throws Exception {
        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("user-1", "polymorphism"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitAnswerReturnsBadRequestWhenNoActiveRound() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new IllegalStateException("No active round for room: room-1"));

        mockMvc.perform(post("/api/game/{roomId}/answer", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new GameController.SubmitAnswerRequest("user-1", "polymorphism"))))
            .andExpect(status().isBadRequest());
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
