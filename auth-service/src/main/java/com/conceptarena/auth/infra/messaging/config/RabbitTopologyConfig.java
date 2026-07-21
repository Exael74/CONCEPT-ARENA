package com.conceptarena.auth.infra.messaging.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares this service's own outbound exchange so OutboxEventPublisher.publishPending() has
 * somewhere real to publish to (audit gap #2/#6: no service declared any RabbitMQ topology in
 * code before this — see docs/event-contracts.md). Declaring it here means it exists on broker
 * startup regardless of whether any consumer has bound a queue to it yet.
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "conceptarena.auth.events";

    @Bean
    public Exchange authEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    /**
     * A4: UserRegistered had no bound queue, so RabbitMQ silently discarded every one — any future
     * cross-service feature (welcome email, analytics, profile bootstrap) would have lost all
     * history. This durable audit queue retains them so a future consumer can attach and read
     * recent events. Bounded (24h TTL + 100k max-length, drop-head) so it can't grow without limit
     * while unconsumed.
     */
    @Bean
    public Queue userRegisteredAuditQueue() {
        return QueueBuilder.durable("auth.user-registered.audit")
            .withArguments(Map.of(
                "x-message-ttl", 86_400_000,
                "x-max-length", 100_000,
                "x-overflow", "drop-head"))
            .build();
    }

    @Bean
    public Binding userRegisteredAuditBinding(Queue userRegisteredAuditQueue, Exchange authEventsExchange) {
        return BindingBuilder.bind(userRegisteredAuditQueue).to(authEventsExchange)
            .with("auth.user-registered").noargs();
    }

    /** Same rationale as userRegisteredAuditQueue: retains UserVerified (account-activation) events. */
    @Bean
    public Queue userVerifiedAuditQueue() {
        return QueueBuilder.durable("auth.user-verified.audit")
            .withArguments(Map.of(
                "x-message-ttl", 86_400_000,
                "x-max-length", 100_000,
                "x-overflow", "drop-head"))
            .build();
    }

    @Bean
    public Binding userVerifiedAuditBinding(Queue userVerifiedAuditQueue, Exchange authEventsExchange) {
        return BindingBuilder.bind(userVerifiedAuditQueue).to(authEventsExchange)
            .with("auth.user-verified").noargs();
    }
}
