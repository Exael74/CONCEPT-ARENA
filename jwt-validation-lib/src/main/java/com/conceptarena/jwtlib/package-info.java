/**
 * Shared JWT validation and cross-service logging utilities.
 * <p>
 * {@link com.conceptarena.jwtlib.JwtValidator} parses/verifies an HS256 token against the shared
 * {@code JWT_SECRET} and returns the subject userId; {@link com.conceptarena.jwtlib.JwtBearerAuthenticationFilter}
 * applies it to HTTP requests. Still to come: {@code WsJwtHandshakeInterceptor} (generic
 * {@code HandshakeInterceptor} for raw WebSocket auth, added when the first WS-handling service is
 * extracted), {@code StompJwtChannelInterceptor} (generic {@code ChannelInterceptor} for STOMP CONNECT
 * auth, added with the api-gateway phase), and {@code MdcScope} (an {@code AutoCloseable} that removes
 * only the MDC keys it set, added during the security-gap consolidation phase).
 */
package com.conceptarena.jwtlib;
