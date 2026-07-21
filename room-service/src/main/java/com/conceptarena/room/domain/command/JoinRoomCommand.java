package com.conceptarena.room.domain.command;

import com.conceptarena.kernel.command.Command;

public record JoinRoomCommand(String roomId, String userId, String inviteCode) implements Command<String> {
}
