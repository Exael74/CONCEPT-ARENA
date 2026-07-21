package com.conceptarena.auth.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.domain.error.DuplicateEmailException;
import com.conceptarena.auth.domain.event.UserRegistered;
import com.conceptarena.kernel.valueobject.PasswordHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpCodeIssuer otpCodeIssuer;

    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserCommandHandler(eventBus, userRepository, passwordEncoder, otpCodeIssuer);
    }

    @Test
    void registersInactiveUserPublishesEventAndIssuesFirstOtpWhenEmailIsAvailable() {
        when(userRepository.existsByEmail("new@escuelaing.edu.co")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");

        RegisterUserCommand command = new RegisterUserCommand(
            new Email("new@escuelaing.edu.co"), PasswordHash.fromPlain("plain-password"));

        String userId = handler.handle(command);

        assertThat(userId).isNotBlank();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
        verify(eventBus).publish(any(UserRegistered.class));
        verify(otpCodeIssuer).issue(eq("new@escuelaing.edu.co"));
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("taken@escuelaing.edu.co")).thenReturn(true);

        RegisterUserCommand command = new RegisterUserCommand(
            new Email("taken@escuelaing.edu.co"), PasswordHash.fromPlain("plain-password"));

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(DuplicateEmailException.class);
        verify(otpCodeIssuer, never()).issue(org.mockito.ArgumentMatchers.anyString());
    }
}
