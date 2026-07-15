package com.conceptarena.room.infra.ws;

import com.conceptarena.jwtlib.JwtValidator;
import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * room-service only needs /ws/lobby here — /ws/game and /ws/signaling moved to their own
 * services (game-engine-service, voice-signaling-service respectively) in later phases.
 */
@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final LobbyWebSocketHandler lobbyWebSocketHandler;
    private final JwtValidator jwtValidator;

    public RawWebSocketConfig(LobbyWebSocketHandler lobbyWebSocketHandler, JwtValidator jwtValidator) {
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
        this.jwtValidator = jwtValidator;
    }

    @Bean
    public WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor() {
        return new WsJwtHandshakeInterceptor(jwtValidator);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lobbyWebSocketHandler, "/ws/lobby")
            .addInterceptors(wsJwtHandshakeInterceptor())
            .setAllowedOriginPatterns("*");
    }
}
