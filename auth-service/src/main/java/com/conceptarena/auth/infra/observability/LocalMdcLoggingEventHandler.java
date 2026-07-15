package com.conceptarena.auth.infra.observability;

import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.app.bus.EventHandler;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import com.conceptarena.auth.domain.event.UserRegistered;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Never calls MDC.clear() — only removes the specific keys it set, so the "requestId" key set
 * earlier in the same thread by CorrelationIdFilter survives. See the security-gap-consolidation
 * ADR for why this matters (the monolith's equivalent class had this bug).
 */
@Component
public class LocalMdcLoggingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LocalMdcLoggingEventHandler.class);
    private final EventBus eventBus;

    public LocalMdcLoggingEventHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(UserRegistered.class, (EventHandler<UserRegistered>) this::onUserRegistered);
        eventBus.subscribe(UserLoggedIn.class, (EventHandler<UserLoggedIn>) this::onUserLoggedIn);
    }

    private void onUserRegistered(UserRegistered event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "UserRegistered");
            MDC.put("userId", event.getAggregateId());
            log.info("User registered: email={}", event.getEmail().value());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("userId");
        }
    }

    private void onUserLoggedIn(UserLoggedIn event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "UserLoggedIn");
            MDC.put("userId", event.getAggregateId());
            log.info("User logged in");
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("userId");
        }
    }
}
