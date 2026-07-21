package com.conceptarena.game.infra.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * C1/C12: guards docs/event-contracts.md against silent drift. Serializes each game-engine event the
 * way OutboxWritingEventHandler does (whole-event Jackson) and asserts the JSON field set matches the
 * documented contract EXACTLY — so renaming/adding/removing a field on an event breaks this test
 * unless the docs (and consumers' mirrored DTOs) are updated in lockstep.
 */
class EventContractSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private JsonNode serialize(Object event) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(event));
    }

    @Test
    void roundStartedMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new RoundStarted("round-1", "room-1", "What is polymorphism?", 2, 30));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "eventId", "occurredOn", "aggregateId", "roomId", "conceptQuestion", "difficulty", "durationSeconds");
    }

    @Test
    void answerSubmittedMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new AnswerSubmitted("round-1", "room-1", "user-1", "poly", "polymorphism"));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "eventId", "occurredOn", "aggregateId", "roomId", "userId", "answerText", "expectedAnswer");
    }

    @Test
    void roundEndedMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new RoundEnded("round-1", "room-1", Map.of("user-1", 10), Map.of("user-1", "CORRECT")));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "eventId", "occurredOn", "aggregateId", "roomId", "scores", "results");
    }

    @Test
    void gameEndedMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new GameEnded("room-1", Map.of("user-1", 42)));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "eventId", "occurredOn", "aggregateId", "roomId", "finalScores");
    }

    private static List<String> fieldNames(JsonNode node) {
        return java.util.stream.StreamSupport
            .stream(java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
            .toList();
    }
}
