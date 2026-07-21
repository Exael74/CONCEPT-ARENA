package com.conceptarena.auth.domain.command;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.kernel.command.Command;

/** Verifies a submitted OTP code and, on success, returns a signed JWT (same token shape as login). */
public record VerifyOtpCommand(Email email, String code) implements Command<String> {
}
