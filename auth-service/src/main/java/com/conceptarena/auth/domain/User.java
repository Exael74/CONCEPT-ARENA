package com.conceptarena.auth.domain;

import com.conceptarena.kernel.valueobject.EntityId;
import com.conceptarena.kernel.valueobject.PasswordHash;
import com.conceptarena.kernel.valueobject.PasswordVerifier;
import java.time.Instant;

public class User {
    private final EntityId id;
    private final Email email;
    private Username username;
    private PasswordHash passwordHash;
    private boolean active;
    private final Instant registeredAt;

    private User(EntityId id, Email email, Username username, PasswordHash passwordHash, boolean active, Instant registeredAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registeredAt = registeredAt;
    }

    /**
     * A freshly registered account starts INACTIVE: it must be activated via email OTP
     * verification (see VerifyOtpCommandHandler) before it can log in — LoginUserCommandHandler
     * and authenticate() below both already gate on isActive(), so this single flag change is
     * what turns OTP into "complete your registration" rather than an alternative login method.
     */
    public static User register(Email email, Username username, PasswordHash passwordHash) {
        return new User(EntityId.generate(), email, username, passwordHash, false, Instant.now());
    }

    public static User restore(EntityId id, Email email, Username username, PasswordHash passwordHash, boolean active, Instant registeredAt) {
        return new User(id, email, username, passwordHash, active, registeredAt);
    }

    public boolean authenticate(PasswordHash rawPassword, PasswordVerifier verifier) {
        return verifier.matches(rawPassword.value(), this.passwordHash.value()) && this.active;
    }

    public void deactivate() {
        this.active = false;
    }

    /** Marks the account active after a successful OTP email verification. */
    public void activate() {
        this.active = true;
    }

    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeUsername(Username newUsername) {
        this.username = newUsername;
    }

    public EntityId getId() { return id; }
    public Email getEmail() { return email; }
    public Username getUsername() { return username; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Instant getRegisteredAt() { return registeredAt; }
}
