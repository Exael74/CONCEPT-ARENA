package com.conceptarena.auth.domain.event;

import com.conceptarena.kernel.event.DomainEvent;

/**
 * Published when a user completes email verification (OTP) and their account becomes active —
 * distinct from {@link UserLoggedIn}: this marks account activation, not a login. VerifyOtpCommandHandler
 * publishes both (activation, then the resulting auto-login), since they are two separate facts.
 */
public class UserVerified extends DomainEvent {
    public UserVerified(String userId) {
        super(userId);
    }
}
