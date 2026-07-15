package com.conceptarena.room.infra.observability;

import com.conceptarena.room.app.bus.EventBus;
import com.conceptarena.room.app.bus.EventHandler;
import com.conceptarena.kernel.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import com.conceptarena.room.domain.event.RoomCreated;
import com.conceptarena.room.domain.event.RoomJoined;
import com.conceptarena.room.domain.event.RoomLeft;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsAdapter {

    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicInteger activeRooms = new AtomicInteger(0);
    private final AtomicInteger concurrentUsers = new AtomicInteger(0);

    public MicrometerMetricsAdapter(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(DomainEvent.class, (EventHandler<DomainEvent>) this::onAnyEvent);

        Gauge.builder("conceptarena.salas.activas", activeRooms, AtomicInteger::get)
            .description("Salas en estado WAITING o IN_GAME")
            .register(meterRegistry);
        Gauge.builder("conceptarena.usuarios.concurrentes", concurrentUsers, AtomicInteger::get)
            .description("Participantes únicos en salas activas")
            .register(meterRegistry);

        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) e -> activeRooms.incrementAndGet());
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) e -> concurrentUsers.incrementAndGet());
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) e -> concurrentUsers.decrementAndGet());
    }

    private void onAnyEvent(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        counters.computeIfAbsent(eventName,
            name -> Counter.builder("conceptarena.events." + name)
                .description("Count of " + name + " events")
                .tag("service", "room-service")
                .register(meterRegistry)).increment();
    }
}
