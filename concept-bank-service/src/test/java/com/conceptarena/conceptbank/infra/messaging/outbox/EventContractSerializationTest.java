package com.conceptarena.conceptbank.infra.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.conceptbank.domain.event.ConceptBankCreated;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** C1/C5: guards docs/event-contracts.md for ConceptBankCreated (whole-event serialization). */
class EventContractSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void conceptBankCreatedMatchesDocumentedContract() throws Exception {
        ConceptBankCreated event = new ConceptBankCreated("bank-1", "OOP", "Software",
            List.of(new Concept("What is polymorphism?", "polymorphism", 2)));
        JsonNode json = mapper.readTree(mapper.writeValueAsString(event));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "eventId", "occurredOn", "aggregateId", "name", "subject", "concepts");
        assertThat(fieldNames(json.get("concepts").get(0)))
            .containsExactlyInAnyOrder("question", "expectedAnswer", "difficulty");
    }

    private static List<String> fieldNames(JsonNode node) {
        return java.util.stream.StreamSupport
            .stream(java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
            .toList();
    }
}
