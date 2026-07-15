package com.conceptarena.core.user.event;

import com.conceptarena.core.shared.event.DomainEvent;
import com.conceptarena.core.shared.valueobject.Email;

public class UserRegistered extends DomainEvent {
    private final Email email;

    public UserRegistered(String userId, Email email) {
        super(userId);
        this.email = email;
    }

    public Email getEmail() { return email; }
}
