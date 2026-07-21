package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resends the account-verification code for a registered-but-not-yet-verified email — e.g. the
 * first code from registration expired or never arrived.
 *
 * Anti-enumeration: this ALWAYS succeeds from the caller's point of view. No code is generated
 * (and no mail sent) when the email is not registered, OR when it belongs to an already-verified
 * account — in both cases the response is identical, so an attacker can't use this endpoint to
 * probe which emails are registered or already active. The IP rate limiter (RateLimitingFilter,
 * covering the otp paths) bounds how fast codes can be requested.
 */
@Service
public class RequestOtpCommandHandler implements CommandHandler<RequestOtpCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(RequestOtpCommandHandler.class);

    private final UserRepository userRepository;
    private final OtpCodeIssuer otpCodeIssuer;

    public RequestOtpCommandHandler(UserRepository userRepository, OtpCodeIssuer otpCodeIssuer) {
        this.userRepository = userRepository;
        this.otpCodeIssuer = otpCodeIssuer;
    }

    @Override
    public Void handle(RequestOtpCommand command) {
        String email = command.email().value();
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (user.isActive()) {
                log.info("OTP requested for an already-verified email — no-op (not leaked to the caller)");
                return;
            }
            otpCodeIssuer.issue(email);
            log.info("OTP re-issued for an unverified registered email");
        }, () -> log.info("OTP requested for an unregistered email — no-op (not leaked to the caller)"));
        return null;
    }
}
