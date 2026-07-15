package com.conceptarena.auth.infra.messaging.outbox;

import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.app.bus.EventHandler;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import com.conceptarena.auth.domain.event.UserRegistered;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * See docs/event-contracts.md and ADR-002. Both events have zero cross-service consumers today,
 * but are still published (see the event-contract table's rationale: cheap given the outbox
 * infra exists regardless, and unblocks future observability/notification consumers).
 */
@Component
public class OutboxWritingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxWritingEventHandler.class);
    private static final String EXCHANGE = "conceptarena.auth.events";

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
        eventBus.subscribe(UserRegistered.class, (EventHandler<UserRegistered>) this::onUserRegistered);
        eventBus.subscribe(UserLoggedIn.class, (EventHandler<UserLoggedIn>) this::onUserLoggedIn);
    }

    private void onUserRegistered(UserRegistered event) {
        write(event.getAggregateId(), "UserRegistered", "auth.user-registered", event);
    }

    private void onUserLoggedIn(UserLoggedIn event) {
        write(event.getAggregateId(), "UserLoggedIn", "auth.user-logged-in", event);
    }

    private void write(String aggregateId, String eventType, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent row = new OutboxEvent(
                UUID.randomUUID().toString(), aggregateId, eventType, EXCHANGE, routingKey, json, Instant.now());
            outboxEventRepository.save(row);
        } catch (Exception e) {
            log.error("Failed to write outbox row for {} (aggregateId={}): {}", eventType, aggregateId, e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize event for outbox: " + eventType, e);
        }
    }
}
