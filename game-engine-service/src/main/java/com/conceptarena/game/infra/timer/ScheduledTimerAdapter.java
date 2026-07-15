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
    private final Map<String, ScheduledFuture<?>> roundTimers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tickTimers = new ConcurrentHashMap<>();

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
        cancelTimers(event.getRoomId());

        // Periodic 1-second tick to /topic/rooms/{id}/timer
        ScheduledFuture<?> tickFuture = scheduler.scheduleAtFixedRate(() -> {
            messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/timer",
                Map.of("type", "TICK", "roundId", event.getAggregateId()));
        }, 1, 1, TimeUnit.SECONDS);
        tickTimers.put(event.getRoomId(), tickFuture);

        // Round end timer
        ScheduledFuture<?> roundFuture = scheduler.schedule(() -> {
            cancelTick(event.getRoomId());

            if (!roundEndGuard.tryClaim(event.getAggregateId())) {
                // GameSaga already ended this round early (all participants answered in time
                // between this task being scheduled and firing); skip the duplicate end.
                log.info("TIMER Round {} already ended early — skipping duplicate timer end.", event.getAggregateId());
                roundTimers.remove(event.getRoomId());
                return;
            }

            Round round = roundRepository.findById(event.getAggregateId()).orElse(null);
            Map<String, Integer> scores = new HashMap<>();
            Map<String, String> results = new HashMap<>();

            if (round != null) {
                round.end();
                for (Map.Entry<String, Answer> entry : round.getAnswers().entrySet()) {
                    String userId = entry.getKey();
                    Answer answer = entry.getValue();
                    int score = ScoringService.calculateScore(round, answer);
                    scores.put(userId, score);
                    results.put(userId, answer.getResult().name());
                }
                roundRepository.save(round);
                log.info("TIMER Round {} ended for room {}. Computed {} scores.", round.getId().value(), event.getRoomId(), scores.size());
            } else {
                log.warn("TIMER Round {} not found in DB when ending.", event.getAggregateId());
            }

            eventBus.publish(new RoundEnded(event.getAggregateId(), event.getRoomId(), scores, results));
            roundTimers.remove(event.getRoomId());
        }, event.getDurationSeconds(), TimeUnit.SECONDS);
        roundTimers.put(event.getRoomId(), roundFuture);

        log.info("TIMER Started {}s timer for room: {}", event.getDurationSeconds(), event.getRoomId());
    }

    @Override
    public void cancelTimers(String roomId) {
        cancelTick(roomId);
        ScheduledFuture<?> roundFuture = roundTimers.remove(roomId);
        if (roundFuture != null) {
            roundFuture.cancel(false);
            log.info("TIMER Cancelled round timer for room: {}", roomId);
        }
    }

    private void cancelTick(String roomId) {
        ScheduledFuture<?> tickFuture = tickTimers.remove(roomId);
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
    }
}
