package com.conceptarena.room.infra.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.room.domain.event.RoomJoined;
import com.conceptarena.room.domain.event.RoomLeft;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * C1/C5: guards docs/event-contracts.md for room's whole-event serialized events (RoomJoined/RoomLeft).
 * NOTE: RoomCreated is serialized from a dedicated RoomCreatedPayload (to omit inviteCode), not the raw
 * event, so its contract is asserted where that payload is built (OutboxWritingEventHandler), not here.
 */
class EventContractSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private JsonNode serialize(Object event) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(event));
    }

    @Test
    void roomJoinedMatchesDocumentedContract() throws Exception {
        assertThat(fieldNames(serialize(new RoomJoined("room-1", "user-1"))))
            .containsExactlyInAnyOrder("eventId", "occurredOn", "aggregateId", "userId");
    }

    @Test
    void roomLeftMatchesDocumentedContract() throws Exception {
        assertThat(fieldNames(serialize(new RoomLeft("room-1", "user-1"))))
            .containsExactlyInAnyOrder("eventId", "occurredOn", "aggregateId", "userId");
    }

    private static List<String> fieldNames(JsonNode node) {
        return java.util.stream.StreamSupport
            .stream(java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
            .toList();
    }
}
