package com.conceptarena.game.domain.error;

/** Thrown when the owner tries to start a game/round for a room that already has an active round. */
public class GameAlreadyInProgressException extends RuntimeException {
    public GameAlreadyInProgressException(String roomId) {
        super("A round is already in progress for room: " + roomId);
    }
}
