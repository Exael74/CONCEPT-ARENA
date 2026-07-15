package com.conceptarena.game.app.readmodel;

import java.util.Optional;

/**
 * Replaces the monolith's direct RoomRepository/Room dependency in StartRoundCommandHandler and
 * SubmitAnswerCommandHandler. Backed by a local, eventually-consistent read-model populated by
 * consuming RoomCreated/RoomJoined/RoomLeft off RabbitMQ — see
 * docs/architecture-decisions/ADR-004-game-engine-read-models.md.
 */
public interface RoomReadModelPort {

    Optional<RoomSnapshot> findByRoomId(String roomId);

    boolean isParticipant(String roomId, String userId);

    /**
     * Purely local bookkeeping — does NOT write back to room-service's own Room.status, unlike
     * the monolith's room.startGame()+roomRepository.save(room). See ADR-004.
     */
    void markGameStarted(String roomId);

    record RoomSnapshot(String roomId, String conceptBankId, int maxParticipants, boolean gameStarted) {}
}
