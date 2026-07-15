package com.conceptarena.room.infra.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint named /ws/room-stomp (not the monolith's shared /ws) so api-gateway can route it to
 * this service specifically once multiple services each have their own STOMP endpoint — see
 * ADR-006. Uses enableSimpleBroker for now; switches to enableStompBrokerRelay against RabbitMQ's
 * STOMP plugin once RabbitMQ is wired in (gap #6 fix, done in the security-gap-consolidation phase).
 */
@Configuration
@EnableWebSocketMessageBroker
public class RoomStompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/room-stomp").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws/room-stomp").setAllowedOriginPatterns("*");
    }
}
