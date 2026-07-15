package com.conceptarena.core.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.shared.valueobject.PasswordVerifier;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final PasswordVerifier EXACT_MATCH = String::equals;

    @Test
    void registerCreatesActiveUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        assertThat(user.isActive()).isTrue();
        assertThat(user.getEmail().value()).isEqualTo("student@escuelaing.edu.co");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getRegisteredAt()).isNotNull();
    }

    @Test
    void authenticateSucceedsWithMatchingPasswordAndActiveUser() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        boolean result = user.authenticate(PasswordHash.fromPlain("hashed"), EXACT_MATCH);
        assertThat(result).isTrue();
    }

    @Test
    void authenticateFailsWithWrongPassword() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
        boolean result = user.authenticate(PasswordHash.fromPlain("wrong"), EXACT_MATCH);
        assertThat(result).isFalse();
    }

    @Test
    void deactivatedUserCannotAuthenticateEvenWithCorrectPassword() {
        User user = User.register(new Email("student@escuelaing.edu.co"), PasswordHash.fromHash("hashed"));
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
