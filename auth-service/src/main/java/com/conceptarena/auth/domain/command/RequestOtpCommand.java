package com.conceptarena.auth.domain.command;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.kernel.command.Command;

/** Requests an OTP email for passwordless login. Returns nothing (never leaks whether the email exists). */
public record RequestOtpCommand(Email email) implements Command<Void> {
}
