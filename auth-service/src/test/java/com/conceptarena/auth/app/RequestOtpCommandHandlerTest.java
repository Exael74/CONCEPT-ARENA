package com.conceptarena.auth.app;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import com.conceptarena.kernel.valueobject.PasswordHash;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestOtpCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpCodeIssuer otpCodeIssuer;

    private RequestOtpCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RequestOtpCommandHandler(userRepository, otpCodeIssuer);
    }

    @Test
    void issuesANewCodeForARegisteredButUnverifiedUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), new Username("student"), PasswordHash.fromPlain("password123"));
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));

        handler.handle(new RequestOtpCommand(new Email("student@escuelaing.edu.co")));

        verify(otpCodeIssuer).issue(eq("student@escuelaing.edu.co"));
    }

    @Test
    void doesNothingAndDoesNotLeakWhenEmailIsNotRegistered() {
        when(userRepository.findByEmail("ghost@escuelaing.edu.co")).thenReturn(Optional.empty());

        handler.handle(new RequestOtpCommand(new Email("ghost@escuelaing.edu.co")));

        verifyNoInteractions(otpCodeIssuer);
    }

    @Test
    void doesNothingAndDoesNotLeakWhenAccountIsAlreadyVerified() {
        User user = User.register(new Email("student@escuelaing.edu.co"), new Username("student"), PasswordHash.fromPlain("password123"));
        user.activate();
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));

        handler.handle(new RequestOtpCommand(new Email("student@escuelaing.edu.co")));

        verify(otpCodeIssuer, never()).issue(org.mockito.ArgumentMatchers.anyString());
    }
}
