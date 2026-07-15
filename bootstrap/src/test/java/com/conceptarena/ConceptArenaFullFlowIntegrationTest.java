package com.conceptarena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.app.game.RoundRepository;
import com.conceptarena.core.game.model.Round;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end coverage of the MVP loop: register -> login -> create concept bank ->
 * create room -> a second user joins -> the round auto-starts (HU-06/HU-07/HU-08) ->
 * both participants answer correctly -> the round ends early and scores accumulate (HU-09).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ConceptArenaFullFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RoundRepository roundRepository;

    @Test
    void studentsCanRegisterCreateARoomPlayARoundAndSeeScores() throws Exception {
        String userId1 = register("host@escuelaing.edu.co", "password123");
        String token1 = login("host@escuelaing.edu.co", "password123");
        String userId2 = register("guest@escuelaing.edu.co", "password123");
        String token2 = login("guest@escuelaing.edu.co", "password123");

        String bankId = createConceptBank(token1);
        String roomId = createRoom(token1, bankId, userId1);

        // Second participant joins -> GameSaga now sees 2 tracked participants and auto-starts round 1.
        joinRoom(token2, roomId, userId2);

        Round activeRound = roundRepository.findActiveRoundByRoomId(roomId)
            .orElseThrow(() -> new AssertionError("Expected a round to auto-start once 2 participants joined"));
        String correctAnswer = activeRound.getExpectedAnswer();

        submitAnswer(token1, roomId, userId1, correctAnswer);
        submitAnswer(token2, roomId, userId2, correctAnswer);

        MvcResult rankingResult = mockMvc.perform(get("/api/game/{roomId}/ranking", roomId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode scores = objectMapper.readTree(rankingResult.getResponse().getContentAsString()).get("data");
        assertThat(scores.get(userId1).asInt()).isGreaterThan(0);
        assertThat(scores.get(userId2).asInt()).isGreaterThan(0);
    }

    private String register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
            .andExpect(status().isCreated())
            .andReturn();
        return dataAsText(result);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        return dataAsText(result);
    }

    private String createConceptBank(String token) throws Exception {
        Map<String, Object> concept1 = Map.of("question", "What is polymorphism?", "expectedAnswer", "polymorphism", "difficulty", 2);
        Map<String, Object> concept2 = Map.of("question", "What is encapsulation?", "expectedAnswer", "encapsulation", "difficulty", 2);
        Map<String, Object> concept3 = Map.of("question", "What is inheritance?", "expectedAnswer", "inheritance", "difficulty", 2);
        Map<String, Object> concept4 = Map.of("question", "What is abstraction?", "expectedAnswer", "abstraction", "difficulty", 2);
        Map<String, Object> concept5 = Map.of("question", "What is a class?", "expectedAnswer", "class", "difficulty", 2);

        MvcResult result = mockMvc.perform(post("/api/concept-banks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "OOP Basics",
                    "subject", "ARSW",
                    "concepts", java.util.List.of(concept1, concept2, concept3, concept4, concept5)))))
            .andExpect(status().isCreated())
            .andReturn();
        return dataAsText(result);
    }

    private String createRoom(String token, String bankId, String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "OOP Study Room",
                    "type", "PUBLIC",
                    "conceptBankId", bankId,
                    "maxParticipants", 4,
                    "userId", userId))))
            .andExpect(status().isCreated())
            .andReturn();
        return dataAsText(result);
    }

    private void joinRoom(String token, String roomId, String userId) throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/join", roomId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
            .andExpect(status().isOk());
    }

    private void submitAnswer(String token, String roomId, String userId, String answerText) throws Exception {
        mockMvc.perform(post("/api/game/{roomId}/answer", roomId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("userId", userId, "answerText", answerText))))
            .andExpect(status().isOk());
    }

    private String dataAsText(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").asText();
    }
}
