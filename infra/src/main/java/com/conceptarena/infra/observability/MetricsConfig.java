package com.conceptarena.infra.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MicrometerMetricsAdapter micrometerMetricsAdapter(
            com.conceptarena.app.bus.EventBus eventBus, MeterRegistry meterRegistry) {
        return new MicrometerMetricsAdapter(eventBus, meterRegistry);
    }
}
