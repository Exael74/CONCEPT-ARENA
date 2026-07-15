package com.conceptarena.room.domain.command;

import com.conceptarena.kernel.command.Command;

public record LeaveRoomCommand(String roomId, String userId) implements Command<Void> {
}
