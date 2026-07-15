package com.conceptarena.infra.observability;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.shared.event.DomainEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LogbackJsonAdapter {

    private static final Logger log = LoggerFactory.getLogger(LogbackJsonAdapter.class);
    private final EventBus eventBus;

    public LogbackJsonAdapter(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(DomainEvent.class, (EventHandler<DomainEvent>) this::onAnyEvent);
    }

    private void onAnyEvent(DomainEvent event) {
        MDC.put("eventId", event.getEventId());
        MDC.put("eventType", event.getClass().getSimpleName());
        MDC.put("aggregateId", event.getAggregateId());
        log.info("EVENT: {}", event.getClass().getSimpleName());
        MDC.clear();
    }
}
