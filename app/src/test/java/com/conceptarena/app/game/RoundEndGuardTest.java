package com.conceptarena.app.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoundEndGuardTest {

    @Test
    void firstClaimSucceedsSecondClaimForSameRoundFails() {
        RoundEndGuard guard = new RoundEndGuard();

        assertThat(guard.tryClaim("round-1")).isTrue();
        assertThat(guard.tryClaim("round-1")).isFalse();
    }

    @Test
    void differentRoundsClaimIndependently() {
        RoundEndGuard guard = new RoundEndGuard();

        assertThat(guard.tryClaim("round-1")).isTrue();
        assertThat(guard.tryClaim("round-2")).isTrue();
    }

    @Test
    void releaseAllowsTheRoundToBeClaimedAgain() {
        RoundEndGuard guard = new RoundEndGuard();
        guard.tryClaim("round-1");

        guard.release("round-1");

        assertThat(guard.tryClaim("round-1")).isTrue();
    }

    @Test
    void onlyOneWinnerWhenManyThreadsRaceForTheSameRound() throws Exception {
        RoundEndGuard guard = new RoundEndGuard();
        int attempts = 50;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        AtomicInteger winners = new AtomicInteger();
        try {
            List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, attempts)
                .<Callable<Void>>mapToObj(i -> () -> {
                    if (guard.tryClaim("round-race")) {
                        winners.incrementAndGet();
                    }
                    return null;
                })
                .toList();
            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }

        assertThat(winners.get()).isEqualTo(1);
    }
}
