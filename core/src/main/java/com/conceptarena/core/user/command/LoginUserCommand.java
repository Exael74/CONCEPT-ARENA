package com.conceptarena.core.user.command;

import com.conceptarena.core.shared.command.Command;
import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.PasswordHash;

public record LoginUserCommand(Email email, PasswordHash passwordHash) implements Command<String> {
}
