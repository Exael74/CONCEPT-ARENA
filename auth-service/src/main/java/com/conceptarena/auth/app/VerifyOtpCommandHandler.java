package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.VerifyOtpCommand;
import com.conceptarena.auth.domain.error.InvalidOtpException;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import com.conceptarena.auth.domain.event.UserVerified;
import org.springframework.stereotype.Service;

/**
 * Verifies a submitted OTP and, on success, activates the account and issues a JWT — this is the
 * "complete your registration" step, not an alternative login: a freshly-registered account is
 * INACTIVE (see User.register()) until this succeeds. Also doubles as the resend/verify path for
 * an account whose first code expired. The code is single-use (consumed on success). A
 * wrong/expired/exhausted code is a 401 (InvalidOtpException); the message is deliberately generic
 * (doesn't distinguish "wrong code" from "no code / expired") to avoid leaking state.
 */
@Service
public class VerifyOtpCommandHandler implements CommandHandler<VerifyOtpCommand, String> {

    private final EventBus eventBus;
    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final TokenService tokenService;

    public VerifyOtpCommandHandler(EventBus eventBus, UserRepository userRepository,
                                   OtpStore otpStore, TokenService tokenService) {
        this.eventBus = eventBus;
        this.userRepository = userRepository;
        this.otpStore = otpStore;
        this.tokenService = tokenService;
    }

    @Override
    public String handle(VerifyOtpCommand command) {
        String email = command.email().value();
        OtpStore.VerifyResult result = otpStore.verifyAndConsume(email, command.code());
        if (result != OtpStore.VerifyResult.VALID) {
            throw new InvalidOtpException("Invalid or expired code");
        }

        // The code is only ever stored for a registered user. Being inactive here is the NORMAL
        // case (a fresh registrant verifying for the first time) — not rejected.
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidOtpException("Invalid or expired code"));

        boolean wasAlreadyActive = user.isActive();
        if (!wasAlreadyActive) {
            user.activate();
            userRepository.save(user);
            eventBus.publish(new UserVerified(user.getId().value()));
        }

        eventBus.publish(new UserLoggedIn(user.getId().value()));
        return tokenService.generateToken(user.getId().value());
    }
}
