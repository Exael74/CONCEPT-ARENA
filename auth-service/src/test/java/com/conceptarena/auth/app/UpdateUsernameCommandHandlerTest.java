package com.conceptarena.auth.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.auth.domain.command.UpdateUsernameCommand;
import com.conceptarena.auth.domain.error.DuplicateUsernameException;
import com.conceptarena.kernel.valueobject.PasswordHash;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUsernameCommandHandlerTest {

    @Mock private UserRepository userRepository;

    private UpdateUsernameCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateUsernameCommandHandler(userRepository);
    }

    private User user() {
        return User.register(new Email("student@escuelaing.edu.co"), new Username("oldname"), PasswordHash.fromHash("hashed"));
    }

    @Test
    void updatesUsernameWhenNewOneIsFree() {
        User user = user();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newname")).thenReturn(false);

        String result = handler.handle(new UpdateUsernameCommand("user-1", new Username("newname")));

        assertThat(result).isEqualTo("newname");
        assertThat(user.getUsername().value()).isEqualTo("newname");
        verify(userRepository).save(user);
    }

    @Test
    void rejectsWhenNewUsernameIsTakenBySomeoneElse() {
        User user = user();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(new UpdateUsernameCommand("user-1", new Username("taken"))))
            .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).save(user);
    }

    @Test
    void allowsNoOpUpdateToTheSameUsername() {
        User user = user();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        String result = handler.handle(new UpdateUsernameCommand("user-1", new Username("oldname")));

        assertThat(result).isEqualTo("oldname");
        verify(userRepository, never()).existsByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(userRepository).save(user);
    }
}
