package com.conceptarena.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * E3: the gateway is the system's entry point, so it is where a correlation id should originate.
 * This global filter guarantees every inbound request carries an {@code X-Request-Id} — reusing a
 * client-supplied one or minting a new UUID — forwards it to the backend services (whose servlet
 * CorrelationIdFilter reads it into MDC and then onto the outbox/RabbitMQ), and echoes it back to
 * the caller. Before this, a request that entered without the header only got an id once it reached
 * a backend service, so the gateway's own access log couldn't be tied to the rest of the trace.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existing = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;

        ServerWebExchange mutated = exchange.mutate()
            .request(r -> r.headers(h -> h.set(HEADER, requestId)))
            .build();
        mutated.getResponse().getHeaders().set(HEADER, requestId);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        // Run early so the id is present for every downstream filter and the proxied request.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
