package com.conceptarena.core.room.command;

import com.conceptarena.core.shared.command.Command;

public record LeaveRoomCommand(String roomId, String userId) implements Command<Void> {
}
