package com.conceptarena.game.infra.observability;

import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.event.AnswerRejected;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import com.conceptarena.kernel.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsAdapter {

    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    // roundId → start time for latency tracking
    private final Map<String, Instant> roundStartTimes = new ConcurrentHashMap<>();

    public MicrometerMetricsAdapter(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(DomainEvent.class, (EventHandler<DomainEvent>) this::onAnyEvent);

        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) e ->
            roundStartTimes.put(e.getAggregateId(), e.getOccurredOn()));
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) e -> {
            Instant start = roundStartTimes.get(e.getAggregateId());
            if (start != null) {
                long latencyMs = Instant.now().toEpochMilli() - start.toEpochMilli();
                // Fix for audit gap #7: the monolith's Timer had no percentile/histogram
                // configuration, so p95/p99 were invisible in Prometheus — only mean/max.
                Timer.builder("conceptarena.latencia.respuesta")
                    .description("Latencia entre RoundStarted y AnswerSubmitted en ms")
                    .publishPercentiles(0.95, 0.99)
                    .publishPercentileHistogram()
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
                .tag("service", "game-engine-service")
                .register(meterRegistry)).increment();
    }
}
