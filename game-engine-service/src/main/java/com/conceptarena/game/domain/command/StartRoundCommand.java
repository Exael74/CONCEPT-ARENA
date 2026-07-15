package com.conceptarena.game.domain.command;

import com.conceptarena.kernel.command.Command;

public record StartRoundCommand(String roomId, String triggeredByUserId) implements Command<Void> {
}
