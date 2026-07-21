package com.conceptarena.game.domain.command;

import com.conceptarena.kernel.command.Command;

public record StartRoundCommand(String roomId, String triggeredByUserId) implements Command<Void> {
    /** Sentinel for GameSaga's own automatic starts (2 players joined / next round) — not a real user, skips the room-owner check. */
    public static final String SYSTEM_TRIGGERED = "system";
}
