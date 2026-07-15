package com.conceptarena.infra.observability;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.game.event.AnswerRejected;
import com.conceptarena.core.game.event.AnswerSubmitted;
import com.conceptarena.core.game.event.RoundEnded;
import com.conceptarena.core.game.event.RoundStarted;
import com.conceptarena.core.room.event.RoomCreated;
import com.conceptarena.core.room.event.RoomJoined;
import com.conceptarena.core.room.event.RoomLeft;
import com.conceptarena.core.shared.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
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
    // roundId → start time for latency tracking
    private final Map<String, Instant> roundStartTimes = new ConcurrentHashMap<>();

    public MicrometerMetricsAdapter(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void subscribe() {
        // Generic event counter
        eventBus.subscribe(DomainEvent.class, (EventHandler<DomainEvent>) this::onAnyEvent);

        // Gauges
        Gauge.builder("conceptarena.salas.activas", activeRooms, AtomicInteger::get)
            .description("Salas en estado WAITING o IN_GAME")
            .register(meterRegistry);
        Gauge.builder("conceptarena.usuarios.concurrentes", concurrentUsers, AtomicInteger::get)
            .description("Participantes únicos en salas activas")
            .register(meterRegistry);

        // Specific event hooks
        eventBus.subscribe(RoomCreated.class, (EventHandler<RoomCreated>) e -> activeRooms.incrementAndGet());
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) e -> concurrentUsers.incrementAndGet());
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) e -> concurrentUsers.decrementAndGet());
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) e ->
            roundStartTimes.put(e.getAggregateId(), e.getOccurredOn()));
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) e -> {
            Instant start = roundStartTimes.get(e.getAggregateId());
            if (start != null) {
                long latencyMs = Instant.now().toEpochMilli() - start.toEpochMilli();
                Timer.builder("conceptarena.latencia.respuesta")
                    .description("Latencia entre RoundStarted y AnswerSubmitted en ms")
                    .register(meterRegistry)
                    .record(java.time.Duration.ofMillis(latencyMs));
            }
        });
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) e -> {
            roundStartTimes.remove(e.getAggregateId());
            counters.computeIfAbsent("rondas.completas",
                n -> Counter.builder("conceptarena.rondas.completas")
                    .description("Total de rondas completadas")
                    .register(meterRegistry)).increment();
        });

        // Makes anti-cheat rejections (expired timer, wrong room, spoofed userId, etc.)
        // visible as a metric instead of only ever surfacing as an HTTP/WS error nobody watches.
        eventBus.subscribe(AnswerRejected.class, (EventHandler<AnswerRejected>) e ->
            Counter.builder("conceptarena.respuestas.rechazadas")
                .description("Respuestas rechazadas por SubmitAnswerCommandHandler, por motivo")
                .tag("motivo", e.getReason())
                .register(meterRegistry)
                .increment());
    }

    private void onAnyEvent(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        counters.computeIfAbsent(eventName,
            name -> Counter.builder("conceptarena.events." + name)
                .description("Count of " + name + " events")
                .register(meterRegistry)).increment();
    }
}

