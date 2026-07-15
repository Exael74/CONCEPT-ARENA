package com.conceptarena.infra.persistence;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.app.game.RoundRepository;
import com.conceptarena.core.game.event.GameEnded;
import com.conceptarena.core.game.model.Answer;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.infra.persistence.jpa.game.SessionResultEntity;
import com.conceptarena.infra.persistence.jpa.game.SpringDataSessionResultRepository;
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
 * Infra-layer handler that persists session results (KPIs) when a game ends.
 * Lives in infra to keep the app module free of JPA entity dependencies.
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
            sessionResultRepository.save(entity);
            log.info("PERSISTED SessionResult: user={}, room={}, points={}, correct={}, incorrect={}",
                userId, roomId, totalPoints, correct, incorrect);
        });
    }
}
