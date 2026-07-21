package com.conceptarena.auth.infra.otp;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.auth.app.OtpStore.VerifyResult;
import org.junit.jupiter.api.Test;

class InMemoryOtpStoreTest {

    private final InMemoryOtpStore store = new InMemoryOtpStore();

    @Test
    void validCodeVerifiesOnceThenIsConsumed() {
        store.store("a@b.co", "123456");

        assertThat(store.verifyAndConsume("a@b.co", "123456")).isEqualTo(VerifyResult.VALID);
        // Single-use: a second verify of the same (now consumed) code is EXPIRED/absent.
        assertThat(store.verifyAndConsume("a@b.co", "123456")).isEqualTo(VerifyResult.EXPIRED);
    }

    @Test
    void unknownEmailIsExpired() {
        assertThat(store.verifyAndConsume("nobody@b.co", "123456")).isEqualTo(VerifyResult.EXPIRED);
    }

    @Test
    void wrongCodeIsInvalidUntilTheAttemptCapThenInvalidated() {
        store.store("a@b.co", "123456");

        // MAX_ATTEMPTS = 5: attempts 1..4 return INVALID, the 5th invalidates the code.
        for (int i = 1; i <= 4; i++) {
            assertThat(store.verifyAndConsume("a@b.co", "000000")).isEqualTo(VerifyResult.INVALID);
        }
        assertThat(store.verifyAndConsume("a@b.co", "000000")).isEqualTo(VerifyResult.TOO_MANY_ATTEMPTS);
        // Code is now gone even though the real one was never submitted.
        assertThat(store.verifyAndConsume("a@b.co", "123456")).isEqualTo(VerifyResult.EXPIRED);
    }

    @Test
    void storeOverwritesAnyPreviousCode() {
        store.store("a@b.co", "111111");
        store.store("a@b.co", "222222");

        assertThat(store.verifyAndConsume("a@b.co", "111111")).isEqualTo(VerifyResult.INVALID);
        assertThat(store.verifyAndConsume("a@b.co", "222222")).isEqualTo(VerifyResult.VALID);
    }
}
