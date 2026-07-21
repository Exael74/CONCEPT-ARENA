package com.conceptarena.game.infra.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S4: caches the read-heavy session-results/ranking queries (SessionQueryService) so a burst of
 * leaderboard reads doesn't hit Postgres on every call. Short write-TTL (30s) bounds staleness
 * without needing explicit eviction — results only change when a game ends, and a 30s-stale
 * leaderboard is acceptable. In a multi-replica deployment each replica keeps its own copy;
 * that is fine for a bounded-staleness read cache (it is not authoritative state).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RESULTS_BY_USER = "sessionResultsByUser";
    public static final String RESULTS_BY_ROOM = "sessionResultsByRoom";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(RESULTS_BY_USER, RESULTS_BY_ROOM);
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10_000));
        return manager;
    }
}
