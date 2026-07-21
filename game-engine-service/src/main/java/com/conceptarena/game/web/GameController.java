package com.conceptarena.game.web;

import com.conceptarena.game.app.GameSaga;
import com.conceptarena.game.app.RoundRepository;
import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.game.domain.error.NotRoomOwnerException;
import com.conceptarena.game.infra.ws.AnswerRateLimiter;
import com.conceptarena.game.web.dto.ApiResponse;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final CommandBus commandBus;
    private final GameSaga gameSaga;
    private final AnswerRateLimiter rateLimiter;
    private final RoundRepository roundRepository;

    public GameController(CommandBus commandBus, GameSaga gameSaga, AnswerRateLimiter rateLimiter,
                          RoundRepository roundRepository) {
        this.commandBus = commandBus;
        this.gameSaga = gameSaga;
        this.rateLimiter = rateLimiter;
        this.roundRepository = roundRepository;
    }

    /**
     * Fix for audit gap #1 (2026-07-15 remediation): {@code triggeredByUserId} used to come
     * straight from the request body, so any authenticated caller could claim to be any user.
     * The userId is now always taken from the authenticated JWT principal (set by
     * JwtBearerAuthenticationFilter, same source GameWebSocketHandler already trusted on the WS
     * path) — the request body no longer carries a userId at all, so there is nothing to spoof.
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<Void>> startRound(@PathVariable String roomId, Principal principal) {
        try {
            commandBus.dispatch(new StartRoundCommand(roomId, principal.getName()));
            return ResponseEntity.ok(ApiResponse.success("Round started", null));
        } catch (NotRoomOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Fix for audit gap #3: the monolith enforced AnswerRateLimiter only on the WS path
     * (GameWebSocketHandler), leaving this REST endpoint free to be spammed — a client could
     * bypass anti-cheat rate limiting entirely by using HTTP instead of the WebSocket. Reuses
     * the same AnswerRateLimiter instance/3-per-second window as the WS path, so "3/sec" has one
     * source of truth regardless of which transport a client uses.
     *
     * Fix for audit gap #1 (2026-07-15 remediation): {@code userId} used to come straight from
     * the request body, unverified against the caller's own JWT — a user authenticated as A
     * could submit {"userId":"B",...} and have it processed as B's answer. Now taken from the
     * authenticated principal, exactly like GameWebSocketHandler already does on the WS path.
     */
    @PostMapping("/{roomId}/answer")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(@PathVariable String roomId,
                                                           @RequestBody SubmitAnswerRequest request,
                                                           Principal principal) {
        String userId = principal.getName();
        if (!rateLimiter.allow(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many answer submissions, slow down"));
        }
        try {
            commandBus.dispatch(new SubmitAnswerCommand(roomId, userId, request.answerText()));
            return ResponseEntity.ok(ApiResponse.success("Answer submitted", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/ranking")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getRanking(@PathVariable String roomId) {
        Map<String, Integer> scores = gameSaga.getScores(roomId);
        if (scores == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("Ranking for room: " + roomId, scores));
    }

    /**
     * REST fallback for "what round is active right now" — STOMP broadcasts (ROUND_STARTED etc.)
     * only reach clients already subscribed at the moment they're sent; a client that connects or
     * (re)subscribes mid-round never receives that past message (no replay in pub/sub), so it has
     * no other way to learn the current question. The frontend should call this on room/game
     * screen load and on WS reconnect, not rely solely on the live stream to always be caught up.
     * expectedAnswer is deliberately omitted, same as concept-bank-service's GET endpoints.
     */
    @GetMapping("/{roomId}/current-round")
    public ResponseEntity<ApiResponse<CurrentRoundResponse>> getCurrentRound(
            @PathVariable String roomId, Principal principal) {
        return roundRepository.findActiveRoundByRoomId(roomId)
            .map(round -> ResponseEntity.ok(ApiResponse.success("Current round", toResponse(round, principal.getName()))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("No active round for room: " + roomId)));
    }

    private CurrentRoundResponse toResponse(Round round, String userId) {
        long remainingSeconds = Math.max(0,
            round.getDuration().getSeconds() - round.getElapsedTime().getSeconds());
        return new CurrentRoundResponse(
            round.getId().value(), round.getConceptQuestion(), round.getDifficulty(),
            round.getDuration().getSeconds(), remainingSeconds, round.getAnswers().containsKey(userId));
    }

    public record SubmitAnswerRequest(String answerText) {}

    public record CurrentRoundResponse(
        String roundId, String question, int difficulty,
        long durationSeconds, long remainingSeconds, boolean alreadyAnswered) {}
}
