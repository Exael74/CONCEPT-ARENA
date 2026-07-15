package com.conceptarena.conceptbank.infra.bus;

import com.conceptarena.conceptbank.app.bus.EventBus;
import com.conceptarena.conceptbank.app.bus.EventHandler;
import com.conceptarena.kernel.event.DomainEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);
    private final Map<Class<?>, List<EventHandler<?>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        log.debug("PUBLISHING EVENT: {}", event.getClass().getSimpleName());
        subscribers.forEach((eventType, handlers) -> {
            if (eventType.isAssignableFrom(event.getClass())) {
                handlers.forEach(handler -> {
                    try {
                        @SuppressWarnings("unchecked")
                        EventHandler<DomainEvent> typedHandler = (EventHandler<DomainEvent>) handler;
                        typedHandler.handle(event);
                    } catch (Exception e) {
                        log.error("Error handling event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
                    }
                });
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add((EventHandler<?>) handler);
        log.debug("SUBSCRIBED {} to {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
    }
}
