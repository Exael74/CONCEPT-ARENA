package com.conceptarena.game.infra.timer;

import com.conceptarena.game.app.RoundEndGuard;
import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.TimerPort;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.ScoringService;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTimerAdapter implements TimerPort {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTimerAdapter.class);

    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final RoundEndGuard roundEndGuard;
    private final SimpMessagingTemplate messaging;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    // One entry per room, holding both timers for that room's CURRENT round. The entry carries its
    // roundId so that a stale round's timeout task — which can still fire after the round ended,
    // since ScheduledFuture.cancel(false) does not stop an already-running task — can tell it's no
    // longer the current round and leave the newer round's timers untouched. Keying/cleaning by
    // roomId alone (the previous design) let a zombie round-N task cancel round N+1's tick (frozen
    // countdown) and untrack its timer (found alongside the duplicate-round bug, 2026-07-21).
    private final Map<String, RoundTimers> timersByRoom = new ConcurrentHashMap<>();

    private record RoundTimers(String roundId, ScheduledFuture<?> tick, ScheduledFuture<?> end) {}

    public ScheduledTimerAdapter(EventBus eventBus, RoundRepository roundRepository,
                                  RoundEndGuard roundEndGuard, SimpMessagingTemplate messaging) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.roundEndGuard = roundEndGuard;
        this.messaging = messaging;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
    }

    private void onRoundStarted(RoundStarted event) {
        String roomId = event.getRoomId();
        String roundId = event.getAggregateId();
        cancelTimers(roomId); // stop the previous round's timers, if any

        // Periodic 1-second tick to /topic/rooms/{id}/timer
        ScheduledFuture<?> tickFuture = scheduler.scheduleAtFixedRate(() ->
            messaging.convertAndSend("/topic/rooms/" + roomId + "/timer",
                Map.of("type", "TICK", "roundId", roundId)),
            1, 1, TimeUnit.SECONDS);

        // Round-end timer
        ScheduledFuture<?> endFuture = scheduler.schedule(
            () -> endRoundOnTimeout(roomId, roundId),
            event.getDurationSeconds(), TimeUnit.SECONDS);

        timersByRoom.put(roomId, new RoundTimers(roundId, tickFuture, endFuture));
        log.info("TIMER Started {}s timer for room {} round {}", event.getDurationSeconds(), roomId, roundId);
    }

    /** Package-private for direct unit testing of the end/skip/cleanup logic without real scheduling. */
    void endRoundOnTimeout(String roomId, String roundId) {
        // Cancel this round's tick and drop its entry, but ONLY if the room is still on this round —
        // if a newer round already replaced the slot, leave its timers alone.
        clearIfCurrent(roomId, roundId);

        if (!roundEndGuard.tryClaim(roundId)) {
            // The early-end path (or an already-fired timer) already ended this round; skip.
            log.info("TIMER Round {} already ended — skipping duplicate timer end.", roundId);
            return;
        }

        Round round = roundRepository.findById(roundId).orElse(null);
        Map<String, Integer> scores = new HashMap<>();
        Map<String, String> results = new HashMap<>();

        if (round != null) {
            round = endAndSaveWithRetry(round, roundId);
            for (Map.Entry<String, Answer> entry : round.getAnswers().entrySet()) {
                String userId = entry.getKey();
                Answer answer = entry.getValue();
                scores.put(userId, ScoringService.calculateScore(round, answer));
                results.put(userId, answer.getResult().name());
            }
            log.info("TIMER Round {} ended for room {}. Computed {} scores.", roundId, roomId, scores.size());
        } else {
            log.warn("TIMER Round {} not found in DB when ending.", roundId);
        }

        eventBus.publish(new RoundEnded(roundId, roomId, scores, results));
    }

    @Override
    public void cancelTimers(String roomId) {
        RoundTimers removed = timersByRoom.remove(roomId);
        if (removed != null) {
            removed.tick().cancel(false);
            removed.end().cancel(false);
            log.info("TIMER Cancelled timers for room {} round {}", roomId, removed.roundId());
        }
    }

    /** Cancels the tick and removes the entry for (roomId, roundId), but only if that round still owns the room's slot. */
    private void clearIfCurrent(String roomId, String roundId) {
        timersByRoom.computeIfPresent(roomId, (room, entry) -> {
            if (entry.roundId().equals(roundId)) {
                entry.tick().cancel(false);
                return null; // this round's timers are done
            }
            return entry; // a newer round owns the slot — don't touch it
        });
    }

    /**
     * A concurrent answer submission can save a newer version of this round between the load
     * above and this save, making this save's version stale (see RoundEntity#version — added
     * 2026-07-21 after this exact race left 2 rounds simultaneously ACTIVE for one room in
     * production). Reload once and retry: end() is idempotent, and the reload picks up whichever
     * answer raced in.
     */
    private Round endAndSaveWithRetry(Round round, String roundId) {
        round.end();
        try {
            return roundRepository.save(round);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException staleRound) {
            Round fresh = roundRepository.findById(roundId).orElseThrow(() -> staleRound);
            fresh.end();
            return roundRepository.save(fresh);
        }
    }
}
