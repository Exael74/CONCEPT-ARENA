package com.conceptarena.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.kernel.valueobject.PasswordHash;
import com.conceptarena.kernel.valueobject.PasswordVerifier;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final PasswordVerifier EXACT_MATCH = String::equals;

    @Test
    void registerCreatesInactiveUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        assertThat(user.isActive()).isFalse();
        assertThat(user.getEmail().value()).isEqualTo("student@escuelaing.edu.co");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getRegisteredAt()).isNotNull();
    }

    @Test
    void activateMarksUserActive() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        user.activate();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void authenticateSucceedsWithMatchingPasswordAndActiveUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        user.activate();
        boolean result = user.authenticate(PasswordHash.fromPlain("hashed"), EXACT_MATCH);
        assertThat(result).isTrue();
    }

    @Test
    void authenticateFailsForUnverifiedUserEvenWithCorrectPassword() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        boolean result = user.authenticate(PasswordHash.fromPlain("hashed"), EXACT_MATCH);
        assertThat(result).isFalse();
    }

    @Test
    void authenticateFailsWithWrongPassword() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        user.activate();
        boolean result = user.authenticate(PasswordHash.fromPlain("wrong"), EXACT_MATCH);
        assertThat(result).isFalse();
    }

    @Test
    void deactivatedUserCannotAuthenticateEvenWithCorrectPassword() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        user.activate();
        user.deactivate();
        boolean result = user.authenticate(PasswordHash.fromPlain("hashed"), EXACT_MATCH);
        assertThat(result).isFalse();
    }

    @Test
    void changePasswordUpdatesHash() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("old"));
        user.changePassword(PasswordHash.fromHash("new"));
        assertThat(user.getPasswordHash().value()).isEqualTo("new");
    }
}
