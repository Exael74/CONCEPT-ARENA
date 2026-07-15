package com.conceptarena.app.user;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.shared.error.InvalidCredentialsException;
import com.conceptarena.core.user.command.LoginUserCommand;
import com.conceptarena.core.user.event.UserLoggedIn;
import com.conceptarena.core.user.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Validates credentials against the DB, and on success returns a JWT token.
 * Returns the generated JWT string (HU-02).
 */
@Service
public class LoginUserCommandHandler implements CommandHandler<LoginUserCommand, String> {

    private final EventBus eventBus;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginUserCommandHandler(EventBus eventBus, UserRepository userRepository,
                                   PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.eventBus = eventBus;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public String handle(LoginUserCommand command) {
        User user = userRepository.findByEmail(command.email().value())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.passwordHash().value(), user.getPasswordHash().value())
                || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        eventBus.publish(new UserLoggedIn(user.getId().value()));
        // Return a signed JWT that the client will include as "Authorization: Bearer <token>"
        return tokenService.generateToken(user.getId().value());
    }
}
