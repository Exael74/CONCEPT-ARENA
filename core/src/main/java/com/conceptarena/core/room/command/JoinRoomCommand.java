package com.conceptarena.core.room.command;

import com.conceptarena.core.shared.command.Command;

public record JoinRoomCommand(String roomId, String userId, String inviteCode) implements Command<Void> {
}
