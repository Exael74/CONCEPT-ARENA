package com.conceptarena.auth.app;

import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/**
 * Generates a 6-digit one-time code, stores it (TTL-bounded, see {@link OtpStore}), and emails it.
 * Shared by RegisterUserCommandHandler (sends the first code right after registration) and
 * RequestOtpCommandHandler (resends one if the first didn't arrive or expired) — factored out so
 * both callers use the identical code-generation/store/send sequence.
 */
@Service
public class OtpCodeIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpStore otpStore;
    private final EmailSender emailSender;

    public OtpCodeIssuer(OtpStore otpStore, EmailSender emailSender) {
        this.otpStore = otpStore;
        this.emailSender = emailSender;
    }

    public void issue(String email) {
        String code = generateCode();
        otpStore.store(email, code);
        emailSender.sendOtpEmail(email, code, OtpStore.TTL);
    }

    /** Cryptographically-random 6-digit code, zero-padded (000000–999999). */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
