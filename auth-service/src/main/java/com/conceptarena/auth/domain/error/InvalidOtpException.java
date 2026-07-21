package com.conceptarena.auth.domain.error;

import com.conceptarena.kernel.error.DomainException;

/** Thrown when a submitted OTP code is wrong, expired, or invalidated by too many attempts. */
public class InvalidOtpException extends DomainException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
