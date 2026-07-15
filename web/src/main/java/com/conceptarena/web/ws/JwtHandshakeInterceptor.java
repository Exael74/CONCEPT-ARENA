package com.conceptarena.web.ws;

import com.conceptarena.app.user.TokenService;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Authenticates the raw (non-STOMP) WebSocket handshake with the JWT the client already
 * holds from REST login, since browsers cannot set the Authorization header on a WebSocket
 * upgrade request. The token travels as a "token" query parameter; on success, the resolved
 * userId is stored under WS_USER_ID_ATTRIBUTE in the session attributes so handlers can
 * trust it instead of whatever userId the client claims in a message payload.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String WS_USER_ID_ATTRIBUTE = "userId";

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final TokenService tokenService;

    public JwtHandshakeInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        Optional<String> userId = token == null ? Optional.empty() : tokenService.validateAndGetUserId(token);

        if (userId.isEmpty()) {
            log.warn("Rejected WebSocket handshake at {}: missing or invalid token", request.getURI().getPath());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(WS_USER_ID_ATTRIBUTE, userId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String param = servletRequest.getServletRequest().getParameter("token");
            if (param != null && !param.isBlank()) {
                return param;
            }
        }
        String query = request.getURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }
}
