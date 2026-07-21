package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.VerifyOtpCommand;
import com.conceptarena.auth.domain.error.InvalidOtpException;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import org.springframework.stereotype.Service;

/**
 * Verifies a submitted OTP and, on success, issues a JWT — the passwordless counterpart of
 * LoginUserCommandHandler. The code is single-use (consumed on success). A wrong/expired/exhausted
 * code is a 401 (InvalidOtpException); the message is deliberately generic (doesn't distinguish
 * "wrong code" from "no code / expired") to avoid leaking state.
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

        // The code is only ever stored for a registered user, but re-check in case the account was
        // deactivated between request and verify.
        User user = userRepository.findByEmail(email)
            .filter(User::isActive)
            .orElseThrow(() -> new InvalidOtpException("Invalid or expired code"));

        eventBus.publish(new UserLoggedIn(user.getId().value()));
        return tokenService.generateToken(user.getId().value());
    }
}
