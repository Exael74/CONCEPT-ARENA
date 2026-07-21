package com.conceptarena.game.domain.error;

/** Thrown when a non-creator tries to start a room's game — only the room's creator may. */
public class NotRoomOwnerException extends RuntimeException {
    public NotRoomOwnerException(String roomId) {
        super("Only the room creator can start the game: " + roomId);
    }
}
