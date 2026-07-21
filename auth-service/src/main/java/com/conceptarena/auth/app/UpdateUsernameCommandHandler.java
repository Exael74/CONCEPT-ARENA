package com.conceptarena.auth.app;

import com.conceptarena.auth.app.bus.CommandHandler;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.UpdateUsernameCommand;
import com.conceptarena.auth.domain.error.DuplicateUsernameException;
import org.springframework.stereotype.Service;

/** Edits the username of an already-authenticated account (userId comes from the JWT subject). */
@Service
public class UpdateUsernameCommandHandler implements CommandHandler<UpdateUsernameCommand, String> {

    private final UserRepository userRepository;

    public UpdateUsernameCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String handle(UpdateUsernameCommand command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + command.userId()));

        String newUsername = command.newUsername().value();
        boolean unchanged = user.getUsername().value().equals(newUsername);
        if (!unchanged && userRepository.existsByUsername(newUsername)) {
            throw new DuplicateUsernameException(newUsername);
        }

        user.changeUsername(command.newUsername());
        userRepository.save(user);
        return newUsername;
    }
}
