package com.conceptarena.auth.domain.command;

import com.conceptarena.auth.domain.Username;
import com.conceptarena.kernel.command.Command;

/** userId comes from the authenticated JWT subject, never from the request body. */
public record UpdateUsernameCommand(String userId, Username newUsername) implements Command<String> {
}
