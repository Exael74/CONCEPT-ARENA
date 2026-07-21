package com.conceptarena.auth.domain.command;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.kernel.command.Command;
import com.conceptarena.kernel.valueobject.PasswordHash;

public record RegisterUserCommand(Email email, Username username, PasswordHash passwordHash) implements Command<String> {
}
