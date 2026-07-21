package com.conceptarena.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * C5/C11: gives api-gateway a real test beyond contextLoads — verifies the entry-point correlation
 * id behavior (E3): mint one when absent, preserve a client-supplied one, and echo it in the response.
 */
class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void mintsRequestIdWhenAbsentAndForwardsAndEchoesIt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/rooms"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> { forwarded.set(ex); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        String downstream = forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(downstream).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER)).isEqualTo(downstream);
    }

    @Test
    void preservesClientSuppliedRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/rooms").header(CorrelationIdGlobalFilter.HEADER, "client-123"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> { forwarded.set(ex); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER)).isEqualTo("client-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER)).isEqualTo("client-123");
    }
}
