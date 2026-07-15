package com.conceptarena.app.bus;

import com.conceptarena.core.shared.event.DomainEvent;

public interface EventBus {
    void publish(DomainEvent event);
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
}
