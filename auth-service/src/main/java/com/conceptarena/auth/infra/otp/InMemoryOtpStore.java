package com.conceptarena.auth.infra.otp;

import com.conceptarena.auth.app.OtpStore;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link OtpStore}: per-JVM map with expiry + attempt counting. Correct for a single
 * instance (dev / tests); a multi-replica deployment uses {@link RedisOtpStore} instead. Active
 * unless app.otp.store=redis.
 */
@Component
@ConditionalOnProperty(name = "app.otp.store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryOtpStore implements OtpStore {

    private final Map<String, Entry> byEmail = new ConcurrentHashMap<>();

    private static final class Entry {
        final String code;
        final Instant expiresAt;
        int attempts;

        Entry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public void store(String email, String code) {
        byEmail.put(email, new Entry(code, Instant.now().plus(TTL)));
    }

    @Override
    public synchronized VerifyResult verifyAndConsume(String email, String code) {
        Entry entry = byEmail.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt)) {
            byEmail.remove(email);
            return VerifyResult.EXPIRED;
        }
        if (entry.code.equals(code)) {
            byEmail.remove(email);
            return VerifyResult.VALID;
        }
        entry.attempts++;
        if (entry.attempts >= MAX_ATTEMPTS) {
            byEmail.remove(email);
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }
        return VerifyResult.INVALID;
    }
}
