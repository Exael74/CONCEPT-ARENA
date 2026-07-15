package com.conceptarena.room.app.bus;

import com.conceptarena.kernel.event.DomainEvent;

public interface EventBus {
    void publish(DomainEvent event);
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
}
