package com.conceptarena.app.bus;

import com.conceptarena.core.shared.event.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
