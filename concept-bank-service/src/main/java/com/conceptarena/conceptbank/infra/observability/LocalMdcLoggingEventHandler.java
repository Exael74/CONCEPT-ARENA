package com.conceptarena.conceptbank.infra.observability;

import com.conceptarena.conceptbank.app.bus.EventBus;
import com.conceptarena.conceptbank.app.bus.EventHandler;
import com.conceptarena.conceptbank.domain.event.ConceptBankCreated;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Structured log handler — populates MDC with event-specific context for the duration of a single
 * log statement, then removes only those keys. Deliberately never calls MDC.clear(): that would
 * wipe the "requestId" key set earlier in the same thread by CorrelationIdFilter (this is the bug
 * the monolith's ObservabilityEventHandlers had — see the security-gap-consolidation ADR — this
 * service is written correctly from the start instead of copying it forward).
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
        eventBus.subscribe(ConceptBankCreated.class, (EventHandler<ConceptBankCreated>) this::onConceptBankCreated);
    }

    private void onConceptBankCreated(ConceptBankCreated event) {
        try {
            MDC.put("eventId", event.getEventId());
            MDC.put("eventType", "ConceptBankCreated");
            log.info("ConceptBank created: name={}, subject={}, concepts={}",
                event.getName(), event.getSubject(), event.getConcepts().size());
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
        }
    }
}
