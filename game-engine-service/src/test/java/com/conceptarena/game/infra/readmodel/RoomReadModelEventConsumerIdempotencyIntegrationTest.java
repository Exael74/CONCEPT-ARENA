package com.conceptarena.game.infra.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end idempotency test THROUGH a real RabbitMQ (Testcontainers) — audit gap #5. The existing
 * RoomReadModelEventConsumerTest invokes the handler directly; this one publishes the SAME RoomJoined
 * event to the broker twice and proves the processed_events ledger (audit gap #6) keeps the read
 * model at exactly one participant row — i.e. an at-least-once redelivery does not double-apply.
 *
 * Named *IntegrationTest (not *IT) so surefire discovers it, and tagged "integration" (needs Docker)
 * so it is excluded from the normal build. Run it with:
 *   mvn -pl game-engine-service -am -Pintegration test
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
class RoomReadModelEventConsumerIdempotencyIntegrationTest {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    // Mirrors room-service's published topology (see docs/event-contracts.md / RabbitTopologyConfig).
    private static final String ROOM_EXCHANGE = "conceptarena.room.events";
    private static final String ROOM_JOINED_ROUTING_KEY = "room.room-joined";

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private JpaParticipantReadModelRepository participantRepository;

    @Test
    void sameRoomJoinedDeliveredTwiceViaBrokerCreatesExactlyOneParticipant() throws Exception {
        String roomId = "room-it-idem";
        String userId = "user-it-idem";
        String payload = "{\"eventId\":\"evt-it-idem\",\"occurredOn\":\"2026-01-01T00:00:00Z\","
            + "\"aggregateId\":\"" + roomId + "\",\"userId\":\"" + userId + "\"}";

        publishRoomJoined(payload);
        publishRoomJoined(payload); // redelivery — the eventId ledger must suppress the duplicate effect

        // The consumer processes asynchronously; wait for the first delivery to land, then give the
        // second one time to (wrongly) duplicate before asserting there is still exactly one row.
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline
                && !participantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            Thread.sleep(200);
        }
        Thread.sleep(1_000);

        long rows = participantRepository.findAll().stream()
            .filter(p -> roomId.equals(p.getRoomId()) && userId.equals(p.getUserId()))
            .count();
        assertThat(rows).isEqualTo(1);
    }

    private void publishRoomJoined(String json) {
        MessageProperties props = new MessageProperties();
        props.setHeader("correlationId", "corr-it-idem");
        rabbitTemplate.send(ROOM_EXCHANGE, ROOM_JOINED_ROUTING_KEY,
            new Message(json.getBytes(StandardCharsets.UTF_8), props));
    }
}
