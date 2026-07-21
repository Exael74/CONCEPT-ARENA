package com.conceptarena.room.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Room/Participant state is authoritative domain data, not a cache — Redis must never silently evict
 * it under memory pressure. Defensively enforces maxmemory-policy=noeviction on this connection (in
 * addition to the docker-compose Redis service config), so a write that would exceed maxmemory fails
 * loudly (OOM) instead of an old room silently disappearing. See ADR-003.
 *
 * Runs on ApplicationReadyEvent on a daemon thread, NOT in a @PostConstruct: opening a Lettuce
 * connection during bean initialization blocks forever because the client's Netty event loop is not
 * started yet at that point (the connect future never completes) — this hung room-service's startup
 * indefinitely in Docker and is the reason the stack had never actually come up. By ApplicationReady
 * the web server is already listening (so this can never gate startup/health), and running it off the
 * main thread means even a slow CONFIG SET can't wedge anything.
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.conceptarena.room.infra.persistence.redis")
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    private final RedisConnectionFactory connectionFactory;

    public RedisConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enforceNoEvictionPolicy() {
        Thread enforcer = new Thread(() -> {
            try {
                connectionFactory.getConnection().serverCommands().setConfig("maxmemory-policy", "noeviction");
                log.info("Redis maxmemory-policy=noeviction enforced on the room store.");
            } catch (Exception e) {
                // Some managed Redis providers disallow CONFIG SET from clients — in that case the
                // policy must be set on the server/docker-compose side instead. Don't fail over it.
                log.warn("Could not set Redis maxmemory-policy=noeviction via CONFIG SET (may need to "
                    + "be set server-side instead): {}", e.getMessage());
            }
        }, "redis-noeviction-enforcer");
        enforcer.setDaemon(true);
        enforcer.start();
    }
}
