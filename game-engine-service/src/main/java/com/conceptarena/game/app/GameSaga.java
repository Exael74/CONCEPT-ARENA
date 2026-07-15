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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 * SINGLE-INSTANCE ONLY: {@code games}, {@code activeRoundByRoom} and {@code answeredByRound}
 * are plain in-memory maps on this singleton bean, not externalized to a shared store (e.g.
 * Redis). This service must run at a single replica until this state is externalized — see the
 * security-gap-consolidation ADR on this limitation.
 */
@Service
public class GameSaga {

    private static final Logger log = LoggerFactory.getLogger(GameSaga.class);

    private final EventBus eventBus;
    private final CommandBus commandBus;
    private final RoundRepository roundRepository;
    private final TimerPort timerPort;
    private final RoundEndGuard roundEndGuard;
    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    // Tracks which roundId is active per room for early-end detection
    private final Map<String, String> activeRoundByRoom = new ConcurrentHashMap<>();
    // Tracks answers received per active round
    private final Map<String, Set<String>> answeredByRound = new ConcurrentHashMap<>();

    private static class GameState {
        final String roomId;
        final Set<String> participants = new HashSet<>();
        int currentRound = 0;
        final int totalRounds = 5;
        final Map<String, Integer> scores = new HashMap<>();
        boolean ended = false;

        GameState(String roomId) { this.roomId = roomId; }
    }

    public GameSaga(EventBus eventBus, CommandBus commandBus, RoundRepository roundRepository,
                     TimerPort timerPort, RoundEndGuard roundEndGuard) {
        this.eventBus = eventBus;
        this.commandBus = commandBus;
        this.roundRepository = roundRepository;
        this.timerPort = timerPort;
        this.roundEndGuard = roundEndGuard;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(RoomJoined.class, (EventHandler<RoomJoined>) this::onRoomJoined);
        eventBus.subscribe(RoomLeft.class, (EventHandler<RoomLeft>) this::onRoomLeft);
        eventBus.subscribe(RoundStarted.class, (EventHandler<RoundStarted>) this::onRoundStarted);
        eventBus.subscribe(AnswerSubmitted.class, (EventHandler<AnswerSubmitted>) this::onAnswerSubmitted);
        eventBus.subscribe(RoundEnded.class, (EventHandler<RoundEnded>) this::onRoundEnded);
    }

    private void onRoomJoined(RoomJoined event) {
        String roomId = event.getAggregateId();
        games.computeIfAbsent(roomId, GameState::new);
        GameState state = games.get(roomId);
        state.participants.add(event.getUserId());
        state.scores.putIfAbsent(event.getUserId(), 0);

        if (state.participants.size() >= 2 && state.currentRound == 0 && !state.ended) {
            log.info("SAGA Starting first round for room {} with {} participants", roomId, state.participants.size());
            commandBus.dispatch(new StartRoundCommand(roomId, "system"));
        }
    }

    private void onRoomLeft(RoomLeft event) {
        String roomId = event.getAggregateId();
        GameState state = games.get(roomId);
        if (state != null) {
            state.participants.remove(event.getUserId());
            if (state.participants.isEmpty()) {
                games.remove(roomId);
                activeRoundByRoom.remove(roomId);
                timerPort.cancelTimers(roomId);
                log.info("SAGA Game removed for empty room {}", roomId);
            }
        }
    }

    private void onRoundStarted(RoundStarted event) {
        String roomId = event.getRoomId();
        String roundId = event.getAggregateId();
        GameState state = games.get(roomId);
        if (state != null) {
            state.currentRound++;
            activeRoundByRoom.put(roomId, roundId);
            answeredByRound.put(roundId, new HashSet<>());
            log.info("SAGA Round {}/{} started for room {}", state.currentRound, state.totalRounds, roomId);
        }
    }

    private void onAnswerSubmitted(AnswerSubmitted event) {
        String roomId = event.getRoomId();
        String roundId = event.getAggregateId();
        GameState state = games.get(roomId);
        if (state == null) return;

        Set<String> answered = answeredByRound.computeIfAbsent(roundId, k -> new HashSet<>());
        answered.add(event.getUserId());

        // HU-07: Early-end — if all active participants have answered, end the round immediately
        if (answered.size() >= state.participants.size() && !state.participants.isEmpty()) {
            log.info("SAGA All {} participants answered round {} — triggering early end", answered.size(), roundId);
            triggerEarlyRoundEnd(roundId, roomId);
        }
    }

    private void triggerEarlyRoundEnd(String roundId, String roomId) {
        activeRoundByRoom.remove(roomId);
        answeredByRound.remove(roundId);
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
        GameState state = games.get(event.getRoomId());
        if (state == null) return;

        // Accumulate scores from the round
        event.getScores().forEach((userId, score) ->
            state.scores.merge(userId, score, Integer::sum)
        );

        answeredByRound.remove(event.getAggregateId());
        roundEndGuard.release(event.getAggregateId());

        if (state.currentRound >= state.totalRounds) {
            state.ended = true;
            games.remove(event.getRoomId());
            log.info("SAGA Game ENDED for room {} — final scores: {}", event.getRoomId(), state.scores);
            eventBus.publish(new GameEnded(state.roomId, new HashMap<>(state.scores)));
        } else {
            commandBus.dispatch(new StartRoundCommand(state.roomId, "system"));
        }
    }

    /** Returns current accumulated scores for the room, or null if no game is active. */
    public Map<String, Integer> getScores(String roomId) {
        GameState state = games.get(roomId);
        return state == null ? null : new HashMap<>(state.scores);
    }
}
