package com.conceptarena.auth.infra.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant registeredAt;

    public UserEntity() {}

    public UserEntity(String id, String email, String username, String passwordHash, boolean active, Instant registeredAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registeredAt = registeredAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}
