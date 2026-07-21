package com.conceptarena.game.infra.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;

/** A4: the per-IP handshake limiter allows up to the cap, then rejects further handshakes. */
class WsConnectionRateLimitInterceptorTest {

    private final WsConnectionRateLimitInterceptor interceptor = new WsConnectionRateLimitInterceptor();

    @Test
    void allowsUpToTenHandshakesFromAnIpThenRejects() {
        for (int i = 1; i <= 10; i++) {
            assertThat(beforeHandshake("10.0.0.1")).as("handshake #" + i).isTrue();
        }
        assertThat(beforeHandshake("10.0.0.1")).as("11th handshake").isFalse();
    }

    @Test
    void countsPerIpIndependently() {
        for (int i = 0; i < 10; i++) {
            beforeHandshake("10.0.0.1");
        }
        // A different IP still has its full budget.
        assertThat(beforeHandshake("10.0.0.2")).isTrue();
    }

    private boolean beforeHandshake(String ip) {
        ServerHttpRequest request = new org.springframework.http.server.ServletServerHttpRequest(
            requestFromIp(ip));
        // wsHandler is unused by the interceptor, so null is fine here.
        return interceptor.beforeHandshake(request, new org.springframework.http.server.ServletServerHttpResponse(
            new org.springframework.mock.web.MockHttpServletResponse()), null, new HashMap<>());
    }

    private org.springframework.mock.web.MockHttpServletRequest requestFromIp(String ip) {
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        // Use X-Forwarded-For (the interceptor's first choice, and the realistic path behind the
        // gateway/nginx). MockHttpServletRequest.getRemoteHost() ignores setRemoteAddr, so relying on
        // the socket address would collapse every IP to "localhost".
        req.addHeader("X-Forwarded-For", ip);
        return req;
    }
}
