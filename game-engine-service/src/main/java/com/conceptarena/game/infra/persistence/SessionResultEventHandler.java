package com.conceptarena.game.infra.persistence;

import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.infra.persistence.jpa.SessionResultEntity;
import com.conceptarena.game.infra.persistence.jpa.SpringDataSessionResultRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Infra-layer handler that persists session results (KPIs) when a game ends. Stays co-located
 * inside game-engine-service (not a separate "reporting" service) because it needs Round/Answer
 * detail that only exists in this service's own database — see ADR-004, §0.
 */
@Component
public class SessionResultEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SessionResultEventHandler.class);
    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final SpringDataSessionResultRepository sessionResultRepository;

    public SessionResultEventHandler(EventBus eventBus, RoundRepository roundRepository,
                                      SpringDataSessionResultRepository sessionResultRepository) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.sessionResultRepository = sessionResultRepository;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(GameEnded.class, (EventHandler<GameEnded>) this::onGameEnded);
    }

    private void onGameEnded(GameEnded event) {
        String roomId = event.getRoomId();
        Map<String, Integer> finalScores = event.getFinalScores();
        List<Round> rounds = roundRepository.findByRoomId(roomId);

        finalScores.forEach((userId, totalPoints) -> {
            if (sessionResultRepository.existsByRoomIdAndUserId(roomId, userId)) {
                // GameEnded already processed for this (roomId, userId) — a redelivered or
                // duplicate event (e.g. from the RoundEnded race) must not double-write a result.
                log.warn("SKIPPED duplicate SessionResult for user={}, room={} — already persisted", userId, roomId);
                return;
            }

            int correct = 0;
            int incorrect = 0;
            long totalTimeMs = 0;

            for (Round round : rounds) {
                Answer answer = round.getAnswers().get(userId);
                if (answer != null) {
                    if (answer.getResult() == Answer.AnswerResult.CORRECT) {
                        correct++;
                    } else {
                        incorrect++;
                    }
                    if (round.getStartedAt() != null && answer.getSubmittedAt() != null) {
                        totalTimeMs += Duration.between(round.getStartedAt(), answer.getSubmittedAt()).toMillis();
                    }
                } else {
                    incorrect++;
                }
            }

            SessionResultEntity entity = new SessionResultEntity(
                UUID.randomUUID().toString(),
                roomId,
                userId,
                totalPoints,
                correct,
                incorrect,
                totalTimeMs,
                Instant.now()
            );
            try {
                sessionResultRepository.save(entity);
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                // A5: a concurrent duplicate GameEnded on another replica won the
                // uq_session_results_room_user constraint first — treat as already-persisted,
                // don't fail the handler (belt-and-suspenders with the existsBy check above).
                log.warn("SKIPPED duplicate SessionResult (unique constraint) for user={}, room={}", userId, roomId);
                return;
            }
            log.info("PERSISTED SessionResult: user={}, room={}, points={}, correct={}, incorrect={}",
                userId, roomId, totalPoints, correct, incorrect);
        });
    }
}
