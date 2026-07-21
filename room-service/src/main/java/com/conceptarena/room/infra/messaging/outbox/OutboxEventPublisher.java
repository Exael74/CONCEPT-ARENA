package com.conceptarena.room.infra.messaging.outbox;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent row : pending) {
            try {
                rabbitTemplate.convertAndSend(row.getExchange(), row.getRoutingKey(), row.getPayload(), message -> {
                    if (row.getCorrelationId() != null) {
                        message.getMessageProperties().setHeader("correlationId", row.getCorrelationId());
                    }
                    return message;
                });
                row.markPublished(Instant.now());
            } catch (Exception e) {
                row.recordFailure(e.getMessage());
                log.warn("Failed to publish outbox event {} ({}), will retry: {}",
                    row.getId(), row.getEventType(), e.getMessage());
            }
        }
        if (!pending.isEmpty()) {
            outboxEventRepository.saveAll(pending);
        }
    }
}
