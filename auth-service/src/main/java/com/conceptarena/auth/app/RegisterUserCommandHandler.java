package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.domain.error.DuplicateEmailException;
import com.conceptarena.auth.domain.event.UserRegistered;
import com.conceptarena.kernel.valueobject.PasswordHash;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserCommandHandler implements CommandHandler<RegisterUserCommand, String> {

    private final EventBus eventBus;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserCommandHandler(EventBus eventBus, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.eventBus = eventBus;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public String handle(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email().value())) {
            throw new DuplicateEmailException(command.email().value());
        }

        String hashedPassword = passwordEncoder.encode(command.passwordHash().value());
        User user = User.register(command.email(), PasswordHash.fromHash(hashedPassword));
        userRepository.save(user);

        eventBus.publish(new UserRegistered(user.getId().value(), user.getEmail()));
        return user.getId().value();
    }
}
