package com.conceptarena.game.domain.command;

import com.conceptarena.kernel.command.Command;

public record SubmitAnswerCommand(String roomId, String userId, String answerText) implements Command<Void> {
}
