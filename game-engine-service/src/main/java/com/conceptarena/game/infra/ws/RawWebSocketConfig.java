package com.conceptarena.game.infra.ws;

import com.conceptarena.jwtlib.JwtValidator;
import com.conceptarena.jwtlib.WsJwtHandshakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final JwtValidator jwtValidator;
    private final WsConnectionRateLimitInterceptor connectionRateLimiter;

    public RawWebSocketConfig(GameWebSocketHandler gameWebSocketHandler, JwtValidator jwtValidator,
                              WsConnectionRateLimitInterceptor connectionRateLimiter) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.jwtValidator = jwtValidator;
        this.connectionRateLimiter = connectionRateLimiter;
    }

    @Bean
    public WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor() {
        return new WsJwtHandshakeInterceptor(jwtValidator);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // A4: connection limiter runs before the JWT interceptor so it sheds excess handshakes early.
        registry.addHandler(gameWebSocketHandler, "/ws/game")
            .addInterceptors(connectionRateLimiter, wsJwtHandshakeInterceptor())
            .setAllowedOriginPatterns("*");
    }
}
