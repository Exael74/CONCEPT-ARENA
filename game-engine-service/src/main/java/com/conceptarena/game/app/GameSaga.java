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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a game session across multiple rounds.
 * - Tracks participants per room.
 * - Does NOT auto-start: the FIRST round begins only when the room's creator explicitly calls
 *   POST /api/game/{roomId}/start (StartRoundCommandHandler enforces creator-only). This lets the
 *   owner wait until everyone has joined so all players receive the first RoundStarted broadcast
 *   together, instead of the game kicking off the instant a 2nd player joined (changed 2026-07-21).
 * - Detects early round-end: when ALL participants have answered, publishes RoundEnded
 *   immediately (HU-07) so the timer doesn't have to expire.
 * - On RoundEnded: accumulates scores and starts the NEXT round automatically (system-triggered)
 *   or ends the game (HU-09).
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
        // Deliberately no auto-start: the first round starts only when the owner calls
        // POST /api/game/{roomId}/start (see class doc). Joining just registers the participant so
        // scoring/early-end work once the owner does start.
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
            round = endAndSaveWithRetry(round, roundId);
            for (Map.Entry<String, Answer> entry : round.getAnswers().entrySet()) {
                String userId = entry.getKey();
                Answer answer = entry.getValue();
                int score = ScoringService.calculateScore(round, answer);
                scores.put(userId, score);
                results.put(userId, answer.getResult().name());
            }
        }

        eventBus.publish(new RoundEnded(roundId, roomId, scores, results));
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
        } catch (ObjectOptimisticLockingFailureException staleRound) {
            Round fresh = roundRepository.findById(roundId).orElseThrow(() -> staleRound);
            fresh.end();
            return roundRepository.save(fresh);
        }
    }

    private void onRoundEnded(RoundEnded event) {
        GameState state = gameStateStore.find(event.getRoomId());
        if (state == null) return;

        // Accumulate scores from the round
        event.getScores().forEach((userId, score) ->
            state.getScores().merge(userId, score, Integer::sum)
        );

        gameStateStore.clearAnswered(event.getAggregateId());
        // NB: the round-end guard is deliberately NOT released here — see RoundEndGuard. Releasing
        // it let this round's own still-running timer re-claim and re-end it, dispatching a
        // duplicate next-round start (two rounds ACTIVE at once → answering blocked for the players
        // split across them). The claim's TTL bounds memory instead.

        if (state.getCurrentRound() >= state.getTotalRounds()) {
            state.setEnded(true);
            gameStateStore.remove(event.getRoomId());
            log.info("SAGA Game ENDED for room {} — final scores: {}", event.getRoomId(), state.getScores());
            eventBus.publish(new GameEnded(state.getRoomId(), new HashMap<>(state.getScores())));
        } else {
            gameStateStore.save(state);
            commandBus.dispatch(new StartRoundCommand(state.getRoomId(), StartRoundCommand.SYSTEM_TRIGGERED));
        }
    }

    /** Returns current accumulated scores for the room, or null if no game is active. */
    public Map<String, Integer> getScores(String roomId) {
        GameState state = gameStateStore.find(roomId);
        return state == null ? null : new HashMap<>(state.getScores());
    }
}
