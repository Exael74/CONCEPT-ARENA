package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generates a 6-digit OTP for a registered email, stores it (TTL-bounded), and emails it.
 *
 * Anti-enumeration: this ALWAYS succeeds from the caller's point of view — if the email is not a
 * registered user, no code is generated and no mail is sent, but the response is identical, so an
 * attacker can't probe which emails are registered. The IP rate limiter (RateLimitingFilter, now
 * covering the otp paths) bounds how fast codes can be requested.
 */
@Service
public class RequestOtpCommandHandler implements CommandHandler<RequestOtpCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(RequestOtpCommandHandler.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final EmailSender emailSender;

    public RequestOtpCommandHandler(UserRepository userRepository, OtpStore otpStore, EmailSender emailSender) {
        this.userRepository = userRepository;
        this.otpStore = otpStore;
        this.emailSender = emailSender;
    }

    @Override
    public Void handle(RequestOtpCommand command) {
        String email = command.email().value();
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            String code = generateCode();
            otpStore.store(email, code);
            emailSender.sendOtpEmail(email, code, OtpStore.TTL);
            log.info("OTP requested for a registered email (code emailed, valid {}m)", OtpStore.TTL.toMinutes());
        }, () -> log.info("OTP requested for an unregistered email — no-op (not leaked to the caller)"));
        return null;
    }

    /** Cryptographically-random 6-digit code, zero-padded (000000–999999). */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
