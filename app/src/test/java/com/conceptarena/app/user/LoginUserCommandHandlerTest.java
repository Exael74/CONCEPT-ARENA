package com.conceptarena.app.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.shared.error.InvalidCredentialsException;
import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.user.command.LoginUserCommand;
import com.conceptarena.core.user.event.UserLoggedIn;
import com.conceptarena.core.user.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginUserCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    private LoginUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginUserCommandHandler(eventBus, userRepository, passwordEncoder, tokenService);
    }

    private User activeUser() {
        return User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
    }

    @Test
    void returnsTokenOnValidCredentials() {
        User user = activeUser();
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);
        when(tokenService.generateToken(user.getId().value())).thenReturn("jwt-token");

        LoginUserCommand command = new LoginUserCommand(
            new Email("student@escuelaing.edu.co"), PasswordHash.fromPlain("plain"));

        String token = handler.handle(command);

        assertThat(token).isEqualTo("jwt-token");
        verify(eventBus).publish(any(UserLoggedIn.class));
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("unknown@escuelaing.edu.co")).thenReturn(Optional.empty());

        LoginUserCommand command = new LoginUserCommand(
            new Email("unknown@escuelaing.edu.co"), PasswordHash.fromPlain("plain"));

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWrongPassword() {
        User user = activeUser();
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginUserCommand command = new LoginUserCommand(
            new Email("student@escuelaing.edu.co"), PasswordHash.fromPlain("wrong"));

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsDeactivatedUserEvenWithCorrectPassword() {
        User user = activeUser();
        user.deactivate();
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("hashed-plain", "hashed")).thenReturn(true);

        LoginUserCommand command = new LoginUserCommand(
            new Email("student@escuelaing.edu.co"), PasswordHash.fromPlain("hashed-plain"));

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(InvalidCredentialsException.class);
    }
}
