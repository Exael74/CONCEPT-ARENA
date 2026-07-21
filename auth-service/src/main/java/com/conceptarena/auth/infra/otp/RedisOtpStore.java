package com.conceptarena.auth.infra.otp;

import com.conceptarena.auth.app.OtpStore;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis-backed {@link OtpStore} (docker profile): shares the code + attempt counter across
 * auth-service replicas, so an OTP requested on one replica can be verified on another. The whole
 * check-count-consume is ONE atomic Lua script (same style as RedisAuthRateLimiter), so concurrent
 * verify attempts can't race past the attempt cap or reuse a consumed code. Active only when
 * app.otp.store=redis.
 */
@Component
@ConditionalOnProperty(name = "app.otp.store", havingValue = "redis")
public class RedisOtpStore implements OtpStore {

    private static final String CODE_KEY = "auth:otp:code:";
    private static final String ATTEMPTS_KEY = "auth:otp:attempts:";

    // KEYS[1]=code key, KEYS[2]=attempts key; ARGV[1]=submitted code, ARGV[2]=max attempts.
    // Returns VALID / INVALID / EXPIRED / TOO_MANY_ATTEMPTS.
    private static final RedisScript<String> VERIFY = new DefaultRedisScript<>(
        "local stored = redis.call('GET', KEYS[1]) "
        + "if not stored then return 'EXPIRED' end "
        + "if stored == ARGV[1] then redis.call('DEL', KEYS[1], KEYS[2]) return 'VALID' end "
        + "local attempts = redis.call('INCR', KEYS[2]) "
        + "if attempts >= tonumber(ARGV[2]) then redis.call('DEL', KEYS[1], KEYS[2]) return 'TOO_MANY_ATTEMPTS' end "
        + "return 'INVALID'",
        String.class);

    private final StringRedisTemplate redis;

    public RedisOtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void store(String email, String code) {
        // Code + a fresh attempts counter, both bounded by the same TTL so nothing lingers.
        redis.opsForValue().set(CODE_KEY + email, code, TTL);
        redis.opsForValue().set(ATTEMPTS_KEY + email, "0", TTL);
    }

    @Override
    public VerifyResult verifyAndConsume(String email, String code) {
        String result = redis.execute(VERIFY, List.of(CODE_KEY + email, ATTEMPTS_KEY + email),
            code, Integer.toString(MAX_ATTEMPTS));
        return result == null ? VerifyResult.EXPIRED : VerifyResult.valueOf(result);
    }
}
