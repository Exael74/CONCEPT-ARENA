package com.conceptarena.app.session;

import java.util.List;

/**
 * Query service for retrieving session results (KPIs) from persistence.
 * Implemented in infra; used by web layer via DIP.
 */
public interface SessionQueryService {
    List<SessionResultDto> getResultsByUser(String userId);
    List<SessionResultDto> getResultsByRoom(String roomId);

    record SessionResultDto(
        String sessionId,
        String roomId,
        String userId,
        int totalPoints,
        int correctAnswers,
        int incorrectAnswers,
        long totalTimeMs,
        java.time.Instant playedAt
    ) {}
}
