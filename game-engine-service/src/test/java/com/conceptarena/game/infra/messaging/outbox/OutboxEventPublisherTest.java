package com.conceptarena.game.infra.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * C4: unit-tests OutboxEventPublisher's publish loop — the happy path (publish + mark published) and,
 * crucially, the previously-untested failure path (RabbitTemplate throws -> recordFailure, no mark,
 * row saved for retry). No broker involved; RabbitTemplate is mocked.
 */
class OutboxEventPublisherTest {

    private OutboxEventRepository repository;
    private RabbitTemplate rabbitTemplate;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new OutboxEventPublisher(repository, rabbitTemplate);
    }

    private OutboxEvent pendingEvent() {
        return new OutboxEvent("evt-1", "room-1", "RoundStarted", "conceptarena.game.events",
            "game.round-started", "{\"aggregateId\":\"room-1\"}", Instant.now(), "corr-1");
    }

    @Test
    void publishesPendingEventThenMarksItPublished() {
        OutboxEvent row = pendingEvent();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));

        publisher.publishPending();

        verify(rabbitTemplate).convertAndSend(eq("conceptarena.game.events"), eq("game.round-started"),
            eq("{\"aggregateId\":\"room-1\"}"), any(MessagePostProcessor.class));
        assertThat(row.getPublishedAt()).isNotNull();
        assertThat(row.getAttempts()).isZero();
        verify(repository).saveAll(List.of(row));
    }

    @Test
    void recordsFailureAndDoesNotMarkPublishedWhenBrokerThrows() {
        OutboxEvent row = pendingEvent();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));
        doThrow(new org.springframework.amqp.AmqpException("broker down"))
            .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), any(MessagePostProcessor.class));

        publisher.publishPending();

        assertThat(row.getPublishedAt()).isNull();          // not marked published — will be retried
        assertThat(row.getAttempts()).isEqualTo(1);          // failure recorded
        assertThat(row.getLastError()).contains("broker down");
        verify(repository).saveAll(List.of(row));            // persisted so the retry/attempt count survives
    }
}
