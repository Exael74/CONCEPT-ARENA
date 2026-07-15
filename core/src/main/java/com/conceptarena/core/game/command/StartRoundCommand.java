package com.conceptarena.core.game.command;

import com.conceptarena.core.shared.command.Command;

public record StartRoundCommand(String roomId, String triggeredByUserId) implements Command<Void> {
}
