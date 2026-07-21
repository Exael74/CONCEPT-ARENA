package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.GameState;
import com.conceptarena.game.app.GameStateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed GameStateStore — audit gap #7 remediation. Externalizes GameSaga's session state
 * so 2+ game-engine-service replicas share the same view of "who's in this room / which round is
 * active / who has answered it", removing the single-instance constraint. Active only when
 * app.game-state.store=redis (see application-docker.yml); every existing unit test runs against
 * InMemoryGameStateStore instead and is unaffected.
 *
 * Concurrency (audit gap #4, now closed): read-modify-write on the GameState JSON blob
 * (loadOrCreate/save) is not atomic on its own, so GameSaga wraps every handler that mutates a
 * room in RedisGameStateLock.runExclusively — the per-room distributed lock that serializes
 * concurrent join/leave/score-merge writes for one room across replicas, removing the
 * last-write-wins race this class would otherwise have. addAnswered/answeredCount (Redis
 * SADD/SCARD) and RoundEndGuard (SETNX via RedisRoundEndGuard) are independently atomic, so the
 * early-round-end trigger stays race-free even outside that lock.
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "redis")
public class RedisGameStateStore implements GameStateStore {

    private static final Logger log = LoggerFactory.getLogger(RedisGameStateStore.class);
    private static final String GAME_KEY_PREFIX = "game:state:";
    private static final String ACTIVE_ROUND_KEY_PREFIX = "game:active-round:";
    private static final String ANSWERED_KEY_PREFIX = "game:answered:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGameStateStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GameState loadOrCreate(String roomId) {
        GameState existing = find(roomId);
        if (existing != null) {
            return existing;
        }
        GameState fresh = new GameState(roomId);
        save(fresh);
        return fresh;
    }

    @Override
    public GameState find(String roomId) {
        String json = redisTemplate.opsForValue().get(GAME_KEY_PREFIX + roomId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GameState.class);
        } catch (Exception e) {
            log.error("Corrupt GameState JSON for room {}, treating as absent: {}", roomId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void save(GameState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(GAME_KEY_PREFIX + state.getRoomId(), json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize GameState for room " + state.getRoomId(), e);
        }
    }

    @Override
    public void remove(String roomId) {
        redisTemplate.delete(GAME_KEY_PREFIX + roomId);
    }

    @Override
    public String getActiveRound(String roomId) {
        return redisTemplate.opsForValue().get(ACTIVE_ROUND_KEY_PREFIX + roomId);
    }

    @Override
    public void setActiveRound(String roomId, String roundId) {
        redisTemplate.opsForValue().set(ACTIVE_ROUND_KEY_PREFIX + roomId, roundId);
    }

    @Override
    public void clearActiveRound(String roomId) {
        redisTemplate.delete(ACTIVE_ROUND_KEY_PREFIX + roomId);
    }

    @Override
    public boolean addAnswered(String roundId, String userId) {
        Long added = redisTemplate.opsForSet().add(ANSWERED_KEY_PREFIX + roundId, userId);
        return added != null && added > 0;
    }

    @Override
    public int answeredCount(String roundId) {
        Long size = redisTemplate.opsForSet().size(ANSWERED_KEY_PREFIX + roundId);
        return size == null ? 0 : size.intValue();
    }

    @Override
    public void clearAnswered(String roundId) {
        redisTemplate.delete(ANSWERED_KEY_PREFIX + roundId);
    }
}
