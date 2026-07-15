package com.conceptarena.core.user.event;

import com.conceptarena.core.shared.event.DomainEvent;

public class UserLoggedIn extends DomainEvent {
    public UserLoggedIn(String userId) {
        super(userId);
    }
}
