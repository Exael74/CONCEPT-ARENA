package com.conceptarena.core.user.model;

import com.conceptarena.core.shared.valueobject.Email;
import com.conceptarena.core.shared.valueobject.EntityId;
import com.conceptarena.core.shared.valueobject.PasswordHash;
import com.conceptarena.core.shared.valueobject.PasswordVerifier;
import java.time.Instant;

public class User {
    private final EntityId id;
    private final Email email;
    private PasswordHash passwordHash;
    private boolean active;
    private final Instant registeredAt;

    private User(EntityId id, Email email, PasswordHash passwordHash, boolean active, Instant registeredAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registeredAt = registeredAt;
    }

    public static User register(Email email, PasswordHash passwordHash) {
        return new User(EntityId.generate(), email, passwordHash, true, Instant.now());
    }

    public static User restore(EntityId id, Email email, PasswordHash passwordHash, boolean active, Instant registeredAt) {
        return new User(id, email, passwordHash, active, registeredAt);
    }

    public boolean authenticate(PasswordHash rawPassword, PasswordVerifier verifier) {
        return verifier.matches(rawPassword.value(), this.passwordHash.value()) && this.active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public EntityId getId() { return id; }
    public Email getEmail() { return email; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Instant getRegisteredAt() { return registeredAt; }
}
