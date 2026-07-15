package com.conceptarena.web.rest.session;

import com.conceptarena.app.session.SessionQueryService;
import com.conceptarena.app.session.SessionQueryService.SessionResultDto;
import com.conceptarena.web.rest.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for querying historical session results (HU-13).
 *
 * Endpoints:
 *   GET /api/sessions/results?userId={id}  — historial del usuario
 *   GET /api/sessions/results/room/{roomId} — resultados de una sala específica
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionQueryService sessionQueryService;

    public SessionController(SessionQueryService sessionQueryService) {
        this.sessionQueryService = sessionQueryService;
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<SessionResultDto>>> getResultsByUser(
            @RequestParam String userId) {
        List<SessionResultDto> results = sessionQueryService.getResultsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Session results for user: " + userId, results));
    }

    @GetMapping("/results/room/{roomId}")
    public ResponseEntity<ApiResponse<List<SessionResultDto>>> getResultsByRoom(
            @PathVariable String roomId) {
        List<SessionResultDto> results = sessionQueryService.getResultsByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Session results for room: " + roomId, results));
    }
}
