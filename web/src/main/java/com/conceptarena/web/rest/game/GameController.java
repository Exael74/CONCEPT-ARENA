package com.conceptarena.web.rest.game;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.app.game.GameSaga;
import com.conceptarena.core.game.command.StartRoundCommand;
import com.conceptarena.core.game.command.SubmitAnswerCommand;
import com.conceptarena.web.rest.dto.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final CommandBus commandBus;
    private final GameSaga gameSaga;

    public GameController(CommandBus commandBus, GameSaga gameSaga) {
        this.commandBus = commandBus;
        this.gameSaga = gameSaga;
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

    @PostMapping("/{roomId}/answer")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(@PathVariable String roomId,
                                                           @RequestBody SubmitAnswerRequest request) {
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

