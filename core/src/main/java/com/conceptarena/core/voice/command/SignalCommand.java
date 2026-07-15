package com.conceptarena.core.voice.command;

import com.conceptarena.core.shared.command.Command;

public record SignalCommand(
    String roomId,
    String fromUserId,
    String toUserId,
    String type,
    String payload
) implements Command<Void> {
}
