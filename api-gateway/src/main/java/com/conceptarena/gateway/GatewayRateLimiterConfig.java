package com.conceptarena.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

/**
 * A10: gateway-level (global) rate limiting, per client IP, as defense-in-depth on top of the
 * per-service limiters (auth/game/room). It caps total inbound request rate at the single entry
 * point — a client hammering across many endpoints is bounded here even if no single service's
 * own limiter trips. Backed by Redis (Spring Cloud Gateway's RedisRateLimiter) so the token buckets
 * are shared across gateway replicas.
 *
 * docker-profile only: the RequestRateLimiter default-filter and this KeyResolver bean are absent in
 * the default/test profile, so the gateway's context (and the contextLoads test) needs no Redis.
 */
@Configuration
@Profile("docker")
public class GatewayRateLimiterConfig {

    /** Key each bucket by the caller's IP (honoring X-Forwarded-For's first hop if present). */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            return Mono.just(exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }
}
