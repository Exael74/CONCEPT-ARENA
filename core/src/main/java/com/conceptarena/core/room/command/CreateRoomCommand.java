package com.conceptarena.core.room.command;

import com.conceptarena.core.room.model.RoomType;
import com.conceptarena.core.shared.command.Command;

public record CreateRoomCommand(
    String name,
    RoomType type,
    String conceptBankId,
    int maxParticipants,
    String userId
) implements Command<String> {
}
