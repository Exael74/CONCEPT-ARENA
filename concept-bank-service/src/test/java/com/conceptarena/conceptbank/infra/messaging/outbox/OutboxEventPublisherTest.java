package com.conceptarena.conceptbank.infra.messaging.outbox;

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

/** C6: OutboxEventPublisher happy path + failure path (recordFailure, no mark), mirrors game-engine's. */
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
        return new OutboxEvent("evt-1", "bank-1", "ConceptBankCreated", "conceptarena.conceptbank.events",
            "conceptbank.concept-bank-created", "{\"aggregateId\":\"bank-1\"}", Instant.now(), "corr-1");
    }

    @Test
    void publishesPendingEventThenMarksItPublished() {
        OutboxEvent row = pendingEvent();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(row));

        publisher.publishPending();

        verify(rabbitTemplate).convertAndSend(eq("conceptarena.conceptbank.events"), eq("conceptbank.concept-bank-created"),
            eq("{\"aggregateId\":\"bank-1\"}"), any(MessagePostProcessor.class));
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

        assertThat(row.getPublishedAt()).isNull();
        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getLastError()).contains("broker down");
        verify(repository).saveAll(List.of(row));
    }
}
