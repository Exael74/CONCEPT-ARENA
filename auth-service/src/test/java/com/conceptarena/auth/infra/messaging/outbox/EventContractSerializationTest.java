package com.conceptarena.auth.infra.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import com.conceptarena.auth.domain.event.UserRegistered;
import com.conceptarena.auth.domain.event.UserVerified;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * C1/C5: guards docs/event-contracts.md for auth's events against silent drift. OutboxWritingEventHandler
 * serializes the whole event (Jackson), so this asserts the produced JSON field set matches the
 * documented contract EXACTLY — a renamed/added/removed field breaks this unless docs + the mirrored
 * consumer DTOs are updated in lockstep.
 */
class EventContractSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private JsonNode serialize(Object event) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(event));
    }

    @Test
    void userRegisteredMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new UserRegistered("user-1", new Email("student@escuelaing.edu.co")));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder("eventId", "occurredOn", "aggregateId", "email");
        // email is a value object serialized as { "value": "..." }
        assertThat(fieldNames(json.get("email"))).containsExactly("value");
    }

    @Test
    void userLoggedInMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new UserLoggedIn("user-1"));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder("eventId", "occurredOn", "aggregateId");
    }

    @Test
    void userVerifiedMatchesDocumentedContract() throws Exception {
        JsonNode json = serialize(new UserVerified("user-1"));
        assertThat(fieldNames(json)).containsExactlyInAnyOrder("eventId", "occurredOn", "aggregateId");
    }

    private static List<String> fieldNames(JsonNode node) {
        return java.util.stream.StreamSupport
            .stream(java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
            .toList();
    }
}
