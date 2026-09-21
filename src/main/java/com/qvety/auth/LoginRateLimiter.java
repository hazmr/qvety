package com.qvety.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 10 failures per identifier (E.164 phone or email) and 30 per IP in any 15 minute window. In-memory: one JVM in the pilot.
 * A second instance would need a shared store (Redis); recorded, not built.
 * Buckets live in bounded caches that drop a key one window after its last use, so unknown identifiers and
 * scanning IPs cannot grow the maps without limit.
 */
@Component
public class LoginRateLimiter {

    static final int PER_EMAIL = 10;
    static final int PER_IP = 30;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final long MAX_KEYS = 100_000;

    private final Cache<String, Bucket> byEmail = newCache();
    private final Cache<String, Bucket> byIp = newCache();

    /** @return seconds to wait, or 0 when the attempt may proceed. */
    public long retryAfterSeconds(String email, String ip) {
        var e = bucket(byEmail, key(email), PER_EMAIL).estimateAbilityToConsume(1);
        var i = bucket(byIp, ip, PER_IP).estimateAbilityToConsume(1);
        long nanos = 0;
        if (!e.canBeConsumed()) nanos = Math.max(nanos, e.getNanosToWaitForRefill());
        if (!i.canBeConsumed()) nanos = Math.max(nanos, i.getNanosToWaitForRefill());
        return nanos == 0 ? 0 : Math.max(1, Duration.ofNanos(nanos).toSeconds());
    }

    public void recordFailure(String email, String ip) {
        bucket(byEmail, key(email), PER_EMAIL).tryConsume(1);
        bucket(byIp, ip, PER_IP).tryConsume(1);
    }

    public void recordSuccess(String email) {
        byEmail.invalidate(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static Cache<String, Bucket> newCache() {
        return Caffeine.newBuilder().expireAfterAccess(WINDOW).maximumSize(MAX_KEYS).build();
    }

    private static Bucket bucket(Cache<String, Bucket> cache, String key, int limit) {
        return cache.get(key, k -> Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(limit).refillIntervally(limit, WINDOW).build())
            .build());
    }
}
