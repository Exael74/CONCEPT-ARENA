package com.conceptarena.game.infra.messaging.config;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology this service depends on (audit gap #2/#6: previously nothing in
 * any service declared exchanges/queues/bindings in code — RoomReadModelEventConsumer's
 * {@code @RabbitListener} queues would never have existed on a fresh broker). See
 * docs/event-contracts.md for the source-of-truth event/routing-key table this mirrors.
 *
 * Declares this service's OWN exchange (conceptarena.game.events, for OutboxEventPublisher) plus
 * idempotent re-declarations of room-service's and concept-bank-service's exchanges — Spring AMQP
 * declares all of this once on startup via RabbitAdmin, and declaring an exchange that already
 * exists with matching type/durability is a no-op, so startup order between services doesn't
 * matter.
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String GAME_EXCHANGE = "conceptarena.game.events";
    private static final String ROOM_EXCHANGE = "conceptarena.room.events";
    private static final String CONCEPTBANK_EXCHANGE = "conceptarena.conceptbank.events";

    // A6: dead-letter exchange + queue. A message the read-model consumer keeps failing on (after
    // retries, with requeue-rejected=false — see application.yml) is dead-lettered here instead of
    // being requeued forever in a poison-message loop. The 4 read-model queues below route to it.
    // NOTE: these queues carry x-dead-letter-exchange args now; on a broker that already had them
    // declared WITHOUT the arg, delete them (or recreate the broker) so they can be re-declared.
    public static final String DLX_EXCHANGE = "conceptarena.game.dlx";
    private static final Map<String, Object> DEAD_LETTER_ARGS = Map.of("x-dead-letter-exchange", DLX_EXCHANGE);

    @Bean
    public Exchange gameEventsExchange() {
        return ExchangeBuilder.topicExchange(GAME_EXCHANGE).durable(true).build();
    }

    @Bean
    public Exchange roomEventsExchange() {
        return ExchangeBuilder.topicExchange(ROOM_EXCHANGE).durable(true).build();
    }

    @Bean
    public Exchange conceptBankEventsExchange() {
        return ExchangeBuilder.topicExchange(CONCEPTBANK_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue roomCreatedReadmodelQueue() {
        return QueueBuilder.durable("game-engine.room.created.readmodel").withArguments(DEAD_LETTER_ARGS).build();
    }

    @Bean
    public Queue roomJoinedReadmodelQueue() {
        return QueueBuilder.durable("game-engine.room.joined.readmodel").withArguments(DEAD_LETTER_ARGS).build();
    }

    @Bean
    public Queue roomLeftReadmodelQueue() {
        return QueueBuilder.durable("game-engine.room.left.readmodel").withArguments(DEAD_LETTER_ARGS).build();
    }

    @Bean
    public Queue conceptBankCreatedReadmodelQueue() {
        return QueueBuilder.durable("game-engine.conceptbank.created.readmodel").withArguments(DEAD_LETTER_ARGS).build();
    }

    // A6: the dead-letter exchange + its catch-all queue. Poison messages from the read-model
    // consumers land here for inspection instead of looping forever.
    @Bean
    public FanoutExchange gameDlxExchange() {
        return new FanoutExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue gameDlq() {
        return QueueBuilder.durable("game-engine.dlq").build();
    }

    @Bean
    public Binding gameDlqBinding(Queue gameDlq, FanoutExchange gameDlxExchange) {
        return BindingBuilder.bind(gameDlq).to(gameDlxExchange);
    }

    // A4: GameEnded had no bound queue, so RabbitMQ discarded every one — future leaderboard/history
    // features would have lost all data. Durable, bounded audit queue retains them for a future
    // consumer (24h TTL + 100k max-length, drop-head).
    @Bean
    public Queue gameEndedAuditQueue() {
        return QueueBuilder.durable("game.game-ended.audit")
            .withArguments(Map.of(
                "x-message-ttl", 86_400_000,
                "x-max-length", 100_000,
                "x-overflow", "drop-head"))
            .build();
    }

    @Bean
    public Binding gameEndedAuditBinding(Queue gameEndedAuditQueue, Exchange gameEventsExchange) {
        return BindingBuilder.bind(gameEndedAuditQueue).to(gameEventsExchange)
            .with("game.game-ended").noargs();
    }

    @Bean
    public Binding roomCreatedBinding(Queue roomCreatedReadmodelQueue, Exchange roomEventsExchange) {
        return BindingBuilder.bind(roomCreatedReadmodelQueue).to(roomEventsExchange)
            .with("room.room-created").noargs();
    }

    @Bean
    public Binding roomJoinedBinding(Queue roomJoinedReadmodelQueue, Exchange roomEventsExchange) {
        return BindingBuilder.bind(roomJoinedReadmodelQueue).to(roomEventsExchange)
            .with("room.room-joined").noargs();
    }

    @Bean
    public Binding roomLeftBinding(Queue roomLeftReadmodelQueue, Exchange roomEventsExchange) {
        return BindingBuilder.bind(roomLeftReadmodelQueue).to(roomEventsExchange)
            .with("room.room-left").noargs();
    }

    @Bean
    public Binding conceptBankCreatedBinding(Queue conceptBankCreatedReadmodelQueue, Exchange conceptBankEventsExchange) {
        return BindingBuilder.bind(conceptBankCreatedReadmodelQueue).to(conceptBankEventsExchange)
            .with("conceptbank.concept-bank-created").noargs();
    }
}
