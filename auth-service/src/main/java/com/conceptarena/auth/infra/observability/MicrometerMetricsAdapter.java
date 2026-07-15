package com.conceptarena.auth.infra.observability;

import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.app.bus.EventHandler;
import com.conceptarena.kernel.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsAdapter {

    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public MicrometerMetricsAdapter(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(DomainEvent.class, (EventHandler<DomainEvent>) this::onAnyEvent);
    }

    private void onAnyEvent(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        counters.computeIfAbsent(eventName,
            name -> Counter.builder("conceptarena.events." + name)
                .description("Count of " + name + " events")
                .tag("service", "auth-service")
                .register(meterRegistry)).increment();
    }
}
