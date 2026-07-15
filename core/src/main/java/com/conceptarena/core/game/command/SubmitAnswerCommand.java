package com.conceptarena.core.game.command;

import com.conceptarena.core.shared.command.Command;

public record SubmitAnswerCommand(String roomId, String userId, String answerText) implements Command<Void> {
}
