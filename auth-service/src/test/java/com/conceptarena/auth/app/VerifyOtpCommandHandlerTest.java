package com.conceptarena.auth.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.auth.app.bus.EventBus;
import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.auth.domain.command.VerifyOtpCommand;
import com.conceptarena.auth.domain.error.InvalidOtpException;
import com.conceptarena.auth.domain.event.UserLoggedIn;
import com.conceptarena.auth.domain.event.UserVerified;
import com.conceptarena.kernel.valueobject.PasswordHash;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifyOtpCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private UserRepository userRepository;
    @Mock private OtpStore otpStore;
    @Mock private TokenService tokenService;

    private VerifyOtpCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VerifyOtpCommandHandler(eventBus, userRepository, otpStore, tokenService);
    }

    /** A freshly-registered account: inactive until OTP verification (User.register()). */
    private User unverifiedUser() {
        return User.register(new Email("student@escuelaing.edu.co"), new Username("student"), PasswordHash.fromPlain("password123"));
    }

    @Test
    void activatesAccountAndPublishesVerifiedAndLoginOnFirstValidCode() {
        User user = unverifiedUser();
        when(otpStore.verifyAndConsume("student@escuelaing.edu.co", "123456")).thenReturn(OtpStore.VerifyResult.VALID);
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user.getId().value())).thenReturn("jwt-token");

        String token = handler.handle(new VerifyOtpCommand(new Email("student@escuelaing.edu.co"), "123456"));

        assertThat(token).isEqualTo("jwt-token");
        assertThat(user.isActive()).isTrue();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().isActive()).isTrue();
        verify(eventBus).publish(any(UserVerified.class));
        verify(eventBus).publish(any(UserLoggedIn.class));
    }

    @Test
    void doesNotReactivateOrRepublishVerifiedWhenAlreadyActive() {
        User user = unverifiedUser();
        user.activate();
        when(otpStore.verifyAndConsume("student@escuelaing.edu.co", "123456")).thenReturn(OtpStore.VerifyResult.VALID);
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user.getId().value())).thenReturn("jwt-token");

        String token = handler.handle(new VerifyOtpCommand(new Email("student@escuelaing.edu.co"), "123456"));

        assertThat(token).isEqualTo("jwt-token");
        verify(userRepository, never()).save(any(User.class));
        verify(eventBus, never()).publish(any(UserVerified.class));
        verify(eventBus).publish(any(UserLoggedIn.class));
    }

    @Test
    void rejectsWrongOrExpiredCodeWith401Semantics() {
        for (OtpStore.VerifyResult bad : new OtpStore.VerifyResult[]{
                OtpStore.VerifyResult.INVALID, OtpStore.VerifyResult.EXPIRED, OtpStore.VerifyResult.TOO_MANY_ATTEMPTS}) {
            when(otpStore.verifyAndConsume("student@escuelaing.edu.co", "000000")).thenReturn(bad);

            assertThatThrownBy(() -> handler.handle(new VerifyOtpCommand(new Email("student@escuelaing.edu.co"), "000000")))
                .isInstanceOf(InvalidOtpException.class);
        }
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void rejectsWhenUserRecordIsMissingDespiteAValidCode() {
        when(otpStore.verifyAndConsume("student@escuelaing.edu.co", "123456")).thenReturn(OtpStore.VerifyResult.VALID);
        when(userRepository.findByEmail("student@escuelaing.edu.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new VerifyOtpCommand(new Email("student@escuelaing.edu.co"), "123456")))
            .isInstanceOf(InvalidOtpException.class);
        verify(tokenService, never()).generateToken(any());
    }
}
