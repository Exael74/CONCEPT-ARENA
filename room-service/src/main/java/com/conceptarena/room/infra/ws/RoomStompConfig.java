package com.conceptarena.room.infra.ws;

import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint named /ws/room-stomp (not the monolith's shared /ws) so api-gateway can route it to
 * this service specifically — see ADR-006.
 *
 * Broker (audit gap #7): the default profile uses an in-process SimpleBroker, which only reaches
 * clients connected to THIS instance — with 2+ replicas a /topic/lobby broadcast would not fan out
 * to everyone (the horizontal-scaling gap StompRoomNotificationAdapter's broadcasts otherwise hit).
 * When app.stomp.relay.enabled=true (docker profile) it instead uses a STOMP broker relay against
 * RabbitMQ's rabbitmq_stomp plugin, so every replica shares the same topics and lobby broadcasts
 * reach all connected clients regardless of which replica they landed on.
 *
 * The handshake reuses WsJwtHandshakeInterceptor: browsers cannot set an Authorization header on a
 * WS upgrade, so the JWT travels as ?token= and the handshake is rejected without a valid one.
 */
@Configuration
@EnableWebSocketMessageBroker
public class RoomStompConfig implements WebSocketMessageBrokerConfigurer {

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

    public RoomStompConfig(WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor) {
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
        registry.addEndpoint("/ws/room-stomp").setAllowedOriginPatterns("*")
            .addInterceptors(wsJwtHandshakeInterceptor).withSockJS();
        registry.addEndpoint("/ws/room-stomp").setAllowedOriginPatterns("*")
            .addInterceptors(wsJwtHandshakeInterceptor);
    }
}
