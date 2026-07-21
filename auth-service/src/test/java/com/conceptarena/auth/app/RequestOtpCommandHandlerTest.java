package com.conceptarena.auth.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import com.conceptarena.kernel.valueobject.PasswordHash;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestOtpCommandHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpStore otpStore;
    @Mock private EmailSender emailSender;

    private RequestOtpCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RequestOtpCommandHandler(userRepository, otpStore, emailSender);
    }

    @Test
    void storesAndEmailsA6DigitCodeForARegisteredUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromPlain("password123"));
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));

        handler.handle(new RequestOtpCommand(new Email("student@escuelaing.edu.co")));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(otpStore).store(eq("student@escuelaing.edu.co"), code.capture());
        org.assertj.core.api.Assertions.assertThat(code.getValue()).matches("\\d{6}");
        verify(emailSender).sendOtpEmail(eq("student@escuelaing.edu.co"), eq(code.getValue()), any(Duration.class));
    }

    @Test
    void doesNothingAndDoesNotLeakWhenEmailIsNotRegistered() {
        when(userRepository.findByEmail("ghost@escuelaing.edu.co")).thenReturn(Optional.empty());

        handler.handle(new RequestOtpCommand(new Email("ghost@escuelaing.edu.co")));

        verifyNoInteractions(otpStore, emailSender);
    }
}
