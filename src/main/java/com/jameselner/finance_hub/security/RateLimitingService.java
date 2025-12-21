package com.jameselner.finance_hub.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting authentication attempts to prevent brute force attacks.
 * Uses a token bucket algorithm with the following limits:
 * - Login: 5 attempts per minute per IP
 * - Registration: 3 attempts per minute per IP
 * - Password reset: 3 attempts per minute per IP
 */
@Service
public class RateLimitingService {

    private static final int LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final int REGISTRATION_ATTEMPTS_PER_MINUTE = 3;
    private static final int PASSWORD_RESET_ATTEMPTS_PER_MINUTE = 3;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registrationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();

    /**
     * Check if a login attempt is allowed for the given IP address.
     *
     * @param ipAddress the client IP address
     * @return true if the attempt is allowed, false if rate limited
     */
    public boolean isLoginAllowed(final String ipAddress) {
        return getBucket(loginBuckets, ipAddress, LOGIN_ATTEMPTS_PER_MINUTE).tryConsume(1);
    }

    /**
     * Check if a registration attempt is allowed for the given IP address.
     *
     * @param ipAddress the client IP address
     * @return true if the attempt is allowed, false if rate limited
     */
    public boolean isRegistrationAllowed(final String ipAddress) {
        return getBucket(registrationBuckets, ipAddress, REGISTRATION_ATTEMPTS_PER_MINUTE).tryConsume(1);
    }

    /**
     * Check if a password reset attempt is allowed for the given IP address.
     *
     * @param ipAddress the client IP address
     * @return true if the attempt is allowed, false if rate limited
     */
    public boolean isPasswordResetAllowed(final String ipAddress) {
        return getBucket(passwordResetBuckets, ipAddress, PASSWORD_RESET_ATTEMPTS_PER_MINUTE).tryConsume(1);
    }

    /**
     * Get the remaining number of login attempts for the given IP address.
     *
     * @param ipAddress the client IP address
     * @return the number of remaining attempts
     */
    public long getRemainingLoginAttempts(final String ipAddress) {
        return getBucket(loginBuckets, ipAddress, LOGIN_ATTEMPTS_PER_MINUTE).getAvailableTokens();
    }

    /**
     * Clear rate limiting data for a specific IP (e.g., after successful authentication).
     *
     * @param ipAddress the client IP address
     */
    public void clearRateLimitForIp(final String ipAddress) {
        loginBuckets.remove(ipAddress);
        registrationBuckets.remove(ipAddress);
        passwordResetBuckets.remove(ipAddress);
    }

    private Bucket getBucket(final Map<String, Bucket> buckets, final String ipAddress, final int limit) {
        return buckets.computeIfAbsent(ipAddress, ip -> createBucket(limit));
    }

    private Bucket createBucket(final int tokensPerMinute) {
        Bandwidth limit = Bandwidth.classic(tokensPerMinute,
                Refill.greedy(tokensPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
