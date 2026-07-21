package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.domain.error.DuplicateEmailException;
import com.conceptarena.auth.domain.error.DuplicateUsernameException;
import com.conceptarena.auth.domain.event.UserRegistered;
import com.conceptarena.kernel.valueobject.PasswordHash;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserCommandHandler implements CommandHandler<RegisterUserCommand, String> {

    private final EventBus eventBus;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpCodeIssuer otpCodeIssuer;

    public RegisterUserCommandHandler(EventBus eventBus, UserRepository userRepository,
                                      PasswordEncoder passwordEncoder, OtpCodeIssuer otpCodeIssuer) {
        this.eventBus = eventBus;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpCodeIssuer = otpCodeIssuer;
    }

    /**
     * Deliberately NOT @Transactional: sending the verification email (OtpCodeIssuer -> EmailSender)
     * is network I/O (SMTP), and this codebase's own outbox pattern exists precisely to avoid doing
     * that kind of I/O inside a DB transaction. userRepository.save() is transactional on its own
     * (Spring Data JPA wraps each repository method); by the time issue() runs, the user row is
     * already committed — if the email send fails/is slow, the account still exists and can request
     * a fresh code via /api/auth/otp/request (RequestOtpCommandHandler).
     */
    @Override
    public String handle(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email().value())) {
            throw new DuplicateEmailException(command.email().value());
        }
        if (userRepository.existsByUsername(command.username().value())) {
            throw new DuplicateUsernameException(command.username().value());
        }

        String hashedPassword = passwordEncoder.encode(command.passwordHash().value());
        // User.register() creates the account INACTIVE — it must be verified (OTP) before login.
        User user = User.register(command.email(), command.username(), PasswordHash.fromHash(hashedPassword));
        userRepository.save(user);

        eventBus.publish(new UserRegistered(user.getId().value(), user.getEmail()));
        otpCodeIssuer.issue(user.getEmail().value());
        return user.getId().value();
    }
}
