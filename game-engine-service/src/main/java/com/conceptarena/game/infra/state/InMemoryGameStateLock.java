package com.conceptarena.game.infra.state;

import com.conceptarena.game.app.GameStateLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default GameStateLock: a per-room {@link ReentrantLock}. Reentrant by nature, so the synchronous
 * nested dispatch described in {@link GameStateLock} never self-deadlocks. Single-JVM only — that
 * is sufficient for the in-memory profile, whose GameState never leaves this process anyway.
 * Active unless app.game-state.store=redis (see application-docker.yml).
 */
@Component
@ConditionalOnProperty(name = "app.game-state.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryGameStateLock implements GameStateLock {

    private final Map<String, ReentrantLock> locksByRoom = new ConcurrentHashMap<>();

    @Override
    public void runExclusively(String roomId, Runnable action) {
        ReentrantLock lock = locksByRoom.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
