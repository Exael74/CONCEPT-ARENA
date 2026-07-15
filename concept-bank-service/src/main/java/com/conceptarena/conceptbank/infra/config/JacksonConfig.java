package com.conceptarena.conceptbank.infra.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Outbox payloads are JSON snapshots taken at write time; if an event's shape changes later,
 * older/newer consumers must tolerate the mismatch instead of failing deserialization outright.
 * See docs/architecture-decisions/ADR-002-outbox-pattern-scheduled-polling.md, risk note on
 * payload versioning.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer outboxTolerantJacksonCustomizer() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
