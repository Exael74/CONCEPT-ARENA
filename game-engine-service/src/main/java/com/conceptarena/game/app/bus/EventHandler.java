package com.conceptarena.game.app.bus;

import com.conceptarena.kernel.event.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
