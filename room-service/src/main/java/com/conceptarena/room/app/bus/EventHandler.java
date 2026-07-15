package com.conceptarena.room.app.bus;

import com.conceptarena.kernel.event.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
