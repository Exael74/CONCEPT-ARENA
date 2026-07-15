package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.LoginUserCommand;
import com.conceptarena.auth.domain.error.InvalidCredentialsException;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates credentials against the DB, and on success returns a JWT token.
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
    @Transactional
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
