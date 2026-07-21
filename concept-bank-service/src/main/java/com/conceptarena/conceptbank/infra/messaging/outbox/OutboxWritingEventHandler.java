package com.conceptarena.conceptbank.infra.messaging.outbox;

import com.conceptarena.conceptbank.app.bus.EventBus;
import com.conceptarena.conceptbank.app.bus.EventHandler;
import com.conceptarena.conceptbank.domain.event.ConceptBankCreated;
import com.conceptarena.conceptbank.infra.security.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Subscribes to every domain event that must cross a service boundary (see
 * docs/event-contracts.md) and writes an OutboxEvent row in the same DB transaction as the
 * command handler that published it — the local EventBus dispatch happens synchronously on the
 * same thread inside the handler's @Transactional method, so this insert commits atomically
 * with the aggregate save. See ADR-002.
 */
@Component
public class OutboxWritingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxWritingEventHandler.class);
    private static final String EXCHANGE = "conceptarena.conceptbank.events";

    private final EventBus eventBus;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxWritingEventHandler(EventBus eventBus, OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper) {
        this.eventBus = eventBus;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(ConceptBankCreated.class, (EventHandler<ConceptBankCreated>) this::onConceptBankCreated);
    }

    private void onConceptBankCreated(ConceptBankCreated event) {
        write(event.getAggregateId(), "ConceptBankCreated", "conceptbank.concept-bank-created", event);
    }

    private void write(String aggregateId, String eventType, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent row = new OutboxEvent(
                UUID.randomUUID().toString(), aggregateId, eventType, EXCHANGE, routingKey, json, Instant.now(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
            outboxEventRepository.save(row);
        } catch (Exception e) {
            // A serialization failure here would silently drop the event from the outbox even
            // though the aggregate save already committed — log loudly, this is a bug to fix,
            // not a transient condition to retry.
            log.error("Failed to write outbox row for {} (aggregateId={}): {}", eventType, aggregateId, e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize event for outbox: " + eventType, e);
        }
    }
}
