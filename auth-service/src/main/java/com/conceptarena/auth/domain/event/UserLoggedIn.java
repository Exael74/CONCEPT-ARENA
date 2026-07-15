package com.conceptarena.auth.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

public class UserLoggedIn extends DomainEvent {
    public UserLoggedIn(String userId) {
        super(userId);
    }
}
