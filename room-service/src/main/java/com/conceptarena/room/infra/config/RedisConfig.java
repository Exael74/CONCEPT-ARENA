package com.conceptarena.room.infra.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Room/Participant state is authoritative domain data, not a cache — Redis must never silently
 * evict it under memory pressure. Defensively enforces maxmemory-policy=noeviction on this
 * connection at startup (in addition to the docker-compose Redis service config doing the same),
 * so a write that would exceed maxmemory fails loudly (OOM) instead of an old room silently
 * disappearing. See ADR-003.
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.conceptarena.room.infra.persistence.redis")
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void enforceNoEvictionPolicy() {
        try {
            connectionFactory.getConnection().serverCommands().setConfig("maxmemory-policy", "noeviction");
        } catch (Exception e) {
            // Some managed Redis providers disallow CONFIG SET from clients — in that case the
            // policy must be set on the server/docker-compose side instead. Don't fail startup
            // over it, but make it loud so it isn't silently missed.
            log.warn("Could not set Redis maxmemory-policy=noeviction via CONFIG SET (may need to be "
                + "set server-side instead): {}", e.getMessage());
        }
    }
}
