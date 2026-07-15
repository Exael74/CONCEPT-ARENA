package com.conceptarena.game.web;

import com.conceptarena.game.app.GameSaga;
import com.conceptarena.game.app.bus.CommandBus;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.game.infra.ws.AnswerRateLimiter;
import com.conceptarena.game.web.dto.ApiResponse;
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

    public GameController(CommandBus commandBus, GameSaga gameSaga, AnswerRateLimiter rateLimiter) {
        this.commandBus = commandBus;
        this.gameSaga = gameSaga;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<Void>> startRound(@PathVariable String roomId,
                                                         @RequestBody StartRoundRequest request) {
        try {
            commandBus.dispatch(new StartRoundCommand(roomId, request.triggeredByUserId()));
            return ResponseEntity.ok(ApiResponse.success("Round started", null));
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
     */
    @PostMapping("/{roomId}/answer")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(@PathVariable String roomId,
                                                           @RequestBody SubmitAnswerRequest request) {
        if (!rateLimiter.allow(request.userId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many answer submissions, slow down"));
        }
        try {
            commandBus.dispatch(new SubmitAnswerCommand(roomId, request.userId(), request.answerText()));
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

    public record StartRoundRequest(String triggeredByUserId) {}
    public record SubmitAnswerRequest(String userId, String answerText) {}
}
