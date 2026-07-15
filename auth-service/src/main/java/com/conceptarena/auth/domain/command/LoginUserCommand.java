package com.conceptarena.auth.domain.command;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.kernel.command.Command;
import com.conceptarena.kernel.valueobject.PasswordHash;

public record LoginUserCommand(Email email, PasswordHash passwordHash) implements Command<String> {
}
