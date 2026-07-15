package com.conceptarena.room.domain.command;

import com.conceptarena.room.domain.RoomType;
import com.conceptarena.kernel.command.Command;

public record CreateRoomCommand(
    String name,
    RoomType type,
    String conceptBankId,
    int maxParticipants,
    String userId
) implements Command<String> {
}
