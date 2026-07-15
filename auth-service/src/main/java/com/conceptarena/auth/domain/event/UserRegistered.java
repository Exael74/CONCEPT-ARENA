package com.conceptarena.auth.domain.event;

import com.conceptarena.auth.domain.Email;
import com.conceptarena.kernel.event.DomainEvent;

public class UserRegistered extends DomainEvent {
    private final Email email;

    public UserRegistered(String userId, Email email) {
        super(userId);
        this.email = email;
    }

    public Email getEmail() { return email; }
}
