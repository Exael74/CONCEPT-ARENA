/**
 * Shared JWT validation and cross-service logging utilities.
 * <p>
 * Filled in during the auth-service extraction (Phase 2): {@code JwtValidator} (parses/verifies an
 * HS256 token against the shared {@code JWT_SECRET} and returns the subject userId),
 * {@code WsJwtHandshakeInterceptor} (generic {@code HandshakeInterceptor} for raw WebSocket auth),
 * {@code StompJwtChannelInterceptor} (generic {@code ChannelInterceptor} for STOMP CONNECT auth), and
 * {@code MdcScope} (an {@code AutoCloseable} that removes only the MDC keys it set, fixing the
 * blanket {@code MDC.clear()} bug found in the monolith's observability handlers).
 */
package com.conceptarena.jwtlib;
