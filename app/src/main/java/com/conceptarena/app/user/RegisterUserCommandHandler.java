package com.conceptarena.app.user;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.shared.error.DuplicateEmailException;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.user.command.RegisterUserCommand;
import com.conceptarena.core.user.event.UserRegistered;
import com.conceptarena.core.user.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
