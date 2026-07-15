package com.conceptarena.web.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Exposes the raw (non-STOMP) WebSocket handlers used by lightweight clients that
 * don't speak STOMP. Real-time room/round/signaling broadcasts still go through the
 * STOMP broker configured in infra's WebSocketConfig (/ws with SockJS + /topic/**).
 */
@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final LobbyWebSocketHandler lobbyWebSocketHandler;
    private final SignalingWebSocketHandler signalingWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public RawWebSocketConfig(GameWebSocketHandler gameWebSocketHandler,
                               LobbyWebSocketHandler lobbyWebSocketHandler,
                               SignalingWebSocketHandler signalingWebSocketHandler,
                               JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
        this.signalingWebSocketHandler = signalingWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOriginPatterns("*");
        registry.addHandler(lobbyWebSocketHandler, "/ws/lobby")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOriginPatterns("*");
        registry.addHandler(signalingWebSocketHandler, "/ws/signaling")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOriginPatterns("*");
    }
}
