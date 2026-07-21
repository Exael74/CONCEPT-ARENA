package com.conceptarena.game.infra.ws;

import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint named /ws/game-stomp so api-gateway can route it to this service — see ADR-006.
 *
 * Broker (audit gap #7): the default profile uses an in-process SimpleBroker (single-instance);
 * when app.stomp.relay.enabled=true (docker profile) it uses a STOMP broker relay against RabbitMQ's
 * rabbitmq_stomp plugin, so round/timer broadcasts (StompGameNotificationAdapter, ScheduledTimerAdapter)
 * to /topic reach clients on every replica, not just the one that produced the message.
 *
 * The handshake reuses WsJwtHandshakeInterceptor: browsers cannot set an Authorization header on a
 * WS upgrade, so the JWT travels as ?token= and the handshake is rejected without a valid one.
 */
@Configuration
@EnableWebSocketMessageBroker
public class GameStompConfig implements WebSocketMessageBrokerConfigurer {

    private final WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor;

    @Value("${app.stomp.relay.enabled:false}")
    private boolean relayEnabled;
    @Value("${app.stomp.relay.host:rabbitmq}")
    private String relayHost;
    @Value("${app.stomp.relay.port:61613}")
    private int relayPort;
    @Value("${app.stomp.relay.login:guest}")
    private String relayLogin;
    @Value("${app.stomp.relay.passcode:guest}")
    private String relayPasscode;

    public GameStompConfig(WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor) {
        this.wsJwtHandshakeInterceptor = wsJwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (relayEnabled) {
            // Shared broker across replicas — /topic broadcasts fan out via RabbitMQ (audit gap #7).
            config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayLogin)
                .setClientPasscode(relayPasscode)
                .setSystemLogin(relayLogin)
                .setSystemPasscode(relayPasscode);
        } else {
            config.enableSimpleBroker("/topic", "/user");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/game-stomp").setAllowedOriginPatterns("*")
            .addInterceptors(wsJwtHandshakeInterceptor).withSockJS();
        registry.addEndpoint("/ws/game-stomp").setAllowedOriginPatterns("*")
            .addInterceptors(wsJwtHandshakeInterceptor);
    }
}
