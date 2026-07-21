package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.GameStateLock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis-backed reentrant per-room lock — audit gap #4 remediation. Serializes GameSaga's
 * load-modify-save of a room's GameState across every game-engine-service replica, closing the
 * last-write-wins race RedisGameStateStore's javadoc previously documented as a residual risk.
 *
 * Acquisition is {@code SET key token NX PX ttl} (atomic on a single Redis), spinning until it wins
 * or the acquire timeout elapses. Release is a compare-and-delete Lua script so a replica only ever
 * deletes its OWN token (never one a later holder acquired after this one's TTL expired). The TTL is
 * a safety net against a replica crashing mid-handler and never releasing.
 *
 * Reentrancy is tracked per thread (see {@link GameStateLock}): the synchronous nested dispatch
 * onRoundEnded -> StartRoundCommand -> RoundStarted -> onRoundStarted runs on one thread that
 * already holds the room's Redis lock, so re-entry must NOT try to re-acquire it.
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "redis")
public class RedisGameStateLock implements GameStateLock {

    private static final String KEY_PREFIX = "game:room-lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration ACQUIRE_TIMEOUT = Duration.ofSeconds(5);
    private static final long RETRY_SLEEP_MS = 25;

    private static final RedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class);

    private final StringRedisTemplate redis;

    /** Room -> the token this thread holds it under. Presence means "this thread owns the lock". */
    private final ThreadLocal<Map<String, String>> heldTokens = ThreadLocal.withInitial(HashMap::new);

    public RedisGameStateLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void runExclusively(String roomId, Runnable action) {
        Map<String, String> mine = heldTokens.get();
        boolean outermost = !mine.containsKey(roomId);
        if (outermost) {
            String token = UUID.randomUUID().toString();
            acquire(roomId, token);
            mine.put(roomId, token);
        }
        try {
            action.run();
        } finally {
            if (outermost) {
                String token = mine.remove(roomId);
                redis.execute(RELEASE_IF_OWNER, List.of(KEY_PREFIX + roomId), token);
            }
        }
    }

    private void acquire(String roomId, String token) {
        String key = KEY_PREFIX + roomId;
        long deadlineNanos = System.nanoTime() + ACQUIRE_TIMEOUT.toNanos();
        while (true) {
            Boolean won = redis.opsForValue().setIfAbsent(key, token, LOCK_TTL);
            if (Boolean.TRUE.equals(won)) {
                return;
            }
            if (System.nanoTime() > deadlineNanos) {
                throw new IllegalStateException("Timed out acquiring room lock for room " + roomId);
            }
            try {
                Thread.sleep(RETRY_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted acquiring room lock for room " + roomId, e);
            }
        }
    }
}
