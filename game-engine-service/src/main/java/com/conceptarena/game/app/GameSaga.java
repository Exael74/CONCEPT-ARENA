package com.conceptarena.game.app;

import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.bus.EventHandler;
import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.ScoringService;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import com.conceptarena.game.domain.event.GameEnded;
import com.conceptarena.game.domain.event.RoomJoined;
import com.conceptarena.game.domain.event.RoomLeft;
import com.conceptarena.game.domain.event.RoundEnded;
import com.conceptarena.game.domain.event.RoundStarted;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a game session across multiple rounds.
 * - Tracks participants per room.
 * - Starts rounds automatically when >=2 participants join.
 * - Detects early round-end: when ALL participants have answered, publishes RoundEnded
 *   immediately (HU-07) so the timer doesn't have to expire.
 * - On RoundEnded: accumulates scores and starts the next round or ends the game (HU-09).
 *
 * Unchanged from the monolith except for RoomJoined/RoomLeft now being the local, re-published
 * events fed by RoomReadModelEventConsumer (see ADR-004) instead of the shared in-process bus —
 * GameSaga itself doesn't know or care about that difference.
 *
 * Audit gap #7 remediation (2026-07-15): session state (participants/scores/current round,
 * which round is active per room, who has answered it) used to live in plain ConcurrentHashMaps
 * on this singleton bean, which is why this service had to run at a single replica — see
 * GameStateStore/RoundEndGuard, now externalizable to Redis (RedisGameStateStore/
 * RedisRoundEndGuard, active when app.game-state.store=redis) so 2+ replicas share one view of
 * this state. Defaults to the in-memory implementations (same behavior as before) otherwise.
 */
@Service
public class GameSaga {

    private static final Logger log = LoggerFactory.getLogger(GameSaga.class);

    private final EventBus eventBus;
    private final CommandBus commandBus;
    private final RoundRepository roundRepository;
    private final TimerPort timerPort;
    private final RoundEndGuard roundEndGuard;
    private final GameStateStore gameStateStore;
    private final GameStateLock gameStateLock;

    public GameSaga(EventBus eventBus, CommandBus commandBus, RoundRepository roundRepository,
                     TimerPort timerPort, RoundEndGuard roundEndGuard, GameStateStore gameStateStore,
                     GameStateLock gameStateLock) {
        this.eventBus = eventBus;
        this.commandBus = commandBus;
        this.roundRepository = roundRepository;
        this.timerPort = timerPort;
        this.roundEndGuard = roundEndGuard;
        this.gameStateStore = gameStateStore;
        this.gameStateLock = gameStateLock;
    }

    @PostConstruct
    public void subscribe() {
        // Each handler runs under the per-room lock (audit gap #4) so every mutation for one room is
        // serialized across replicas. The lock is reentrant, so the synchronous nested dispatch
        // (onRoundEnded -> StartRoundCommand -> RoundStarted -> onRoundStarted) does not self-deadlock.
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) e ->
            gameStateLock.runExclusively(e.getAggregateId(), () -> onRoomJoined(e)));
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) e ->
            gameStateLock.runExclusively(e.getAggregateId(), () -> onRoomLeft(e)));
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) e ->
            gameStateLock.runExclusively(e.getRoomId(), () -> onRoundStarted(e)));
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) e ->
            gameStateLock.runExclusively(e.getRoomId(), () -> onAnswerSubmitted(e)));
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) e ->
            gameStateLock.runExclusively(e.getRoomId(), () -> onRoundEnded(e)));
    }

    private void onRoomJoined(RoomJoined event) {
        String roomId = event.getAggregateId();
        GameState state = gameStateStore.loadOrCreate(roomId);
        state.getParticipants().add(event.getUserId());
        state.getScores().putIfAbsent(event.getUserId(), 0);
        gameStateStore.save(state);

        if (state.getParticipants().size() >= 2 && state.getCurrentRound() == 0 && !state.isEnded()) {
            log.info("SAGA Starting first round for room {} with {} participants", roomId, state.getParticipants().size());
            commandBus.dispatch(new StartRoundCommand(roomId, "system"));
        }
    }

    private void onRoomLeft(RoomLeft event) {
        String roomId = event.getAggregateId();
        GameState state = gameStateStore.find(roomId);
        if (state != null) {
            state.getParticipants().remove(event.getUserId());
            if (state.getParticipants().isEmpty()) {
                gameStateStore.remove(roomId);
                gameStateStore.clearActiveRound(roomId);
                timerPort.cancelTimers(roomId);
                log.info("SAGA Game removed for empty room {}", roomId);
            } else {
                gameStateStore.save(state);
            }
        }
    }

    private void onRoundStarted(RoundStarted event) {
        String roomId = event.getRoomId();
        String roundId = event.getAggregateId();
        GameState state = gameStateStore.find(roomId);
        if (state != null) {
            state.setCurrentRound(state.getCurrentRound() + 1);
            gameStateStore.save(state);
            gameStateStore.setActiveRound(roomId, roundId);
            gameStateStore.clearAnswered(roundId);
            log.info("SAGA Round {}/{} started for room {}", state.getCurrentRound(), state.getTotalRounds(), roomId);
        }
    }

    private void onAnswerSubmitted(AnswerSubmitted event) {
        String roomId = event.getRoomId();
        String roundId = event.getAggregateId();
        GameState state = gameStateStore.find(roomId);
        if (state == null) return;

        gameStateStore.addAnswered(roundId, event.getUserId());
        int answeredCount = gameStateStore.answeredCount(roundId);

        // HU-07: Early-end — if all active participants have answered, end the round immediately
        if (answeredCount >= state.getParticipants().size() && !state.getParticipants().isEmpty()) {
            log.info("SAGA All {} participants answered round {} — triggering early end", answeredCount, roundId);
            triggerEarlyRoundEnd(roundId, roomId);
        }
    }

    private void triggerEarlyRoundEnd(String roundId, String roomId) {
        gameStateStore.clearActiveRound(roomId);
        gameStateStore.clearAnswered(roundId);
        timerPort.cancelTimers(roomId);

        if (!roundEndGuard.tryClaim(roundId)) {
            // The scheduled timer already won the race and ended this round; nothing left to do.
            log.info("SAGA Round {} already ended by the timer path — skipping duplicate early end", roundId);
            return;
        }

        Round round = roundRepository.findById(roundId).orElse(null);
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
        }

        eventBus.publish(new RoundEnded(roundId, roomId, scores, results));
    }

    private void onRoundEnded(RoundEnded event) {
        GameState state = gameStateStore.find(event.getRoomId());
        if (state == null) return;

        // Accumulate scores from the round
        event.getScores().forEach((userId, score) ->
            state.getScores().merge(userId, score, Integer::sum)
        );

        gameStateStore.clearAnswered(event.getAggregateId());
        roundEndGuard.release(event.getAggregateId());

        if (state.getCurrentRound() >= state.getTotalRounds()) {
            state.setEnded(true);
            gameStateStore.remove(event.getRoomId());
            log.info("SAGA Game ENDED for room {} — final scores: {}", event.getRoomId(), state.getScores());
            eventBus.publish(new GameEnded(state.getRoomId(), new HashMap<>(state.getScores())));
        } else {
            gameStateStore.save(state);
            commandBus.dispatch(new StartRoundCommand(state.getRoomId(), "system"));
        }
    }

    /** Returns current accumulated scores for the room, or null if no game is active. */
    public Map<String, Integer> getScores(String roomId) {
        GameState state = gameStateStore.find(roomId);
        return state == null ? null : new HashMap<>(state.getScores());
    }
}
