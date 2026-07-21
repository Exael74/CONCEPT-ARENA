package com.conceptarena.room.infra.messaging.config;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares this service's own outbound exchange so OutboxEventPublisher.publishPending() has
 * somewhere real to publish to (audit gap #2/#6: no service declared any RabbitMQ topology in
 * code before this — see docs/event-contracts.md). game-engine-service's consumer config
 * idempotently re-declares the same exchange (matching type/durability) so its bindings work
 * regardless of which service starts first.
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "conceptarena.room.events";

    @Bean
    public Exchange roomEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }
}
