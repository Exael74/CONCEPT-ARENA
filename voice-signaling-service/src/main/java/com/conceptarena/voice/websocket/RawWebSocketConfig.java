package com.conceptarena.voice.websocket;

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

    private final SignalingWebSocketHandler signalingWebSocketHandler;
    private final JwtValidator jwtValidator;

    public RawWebSocketConfig(SignalingWebSocketHandler signalingWebSocketHandler, JwtValidator jwtValidator) {
        this.signalingWebSocketHandler = signalingWebSocketHandler;
        this.jwtValidator = jwtValidator;
    }

    @Bean
    public WsJwtHandshakeInterceptor wsJwtHandshakeInterceptor() {
        return new WsJwtHandshakeInterceptor(jwtValidator);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingWebSocketHandler, "/ws/signaling")
            .addInterceptors(wsJwtHandshakeInterceptor())
            .setAllowedOriginPatterns("*");
    }
}
