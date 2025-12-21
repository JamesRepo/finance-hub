package com.jameselner.finance_hub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    @DisplayName("Login should be allowed within rate limit")
    void loginAllowedWithinLimit() {
        String ip = "192.168.1.1";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.isLoginAllowed(ip),
                    "Login attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Login should be blocked after exceeding rate limit")
    void loginBlockedAfterExceedingLimit() {
        String ip = "192.168.1.2";

        for (int i = 0; i < 5; i++) {
            rateLimitingService.isLoginAllowed(ip);
        }

        assertFalse(rateLimitingService.isLoginAllowed(ip),
                "6th login attempt should be blocked");
    }

    @Test
    @DisplayName("Registration should be allowed within rate limit")
    void registrationAllowedWithinLimit() {
        String ip = "192.168.1.3";

        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitingService.isRegistrationAllowed(ip),
                    "Registration attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Registration should be blocked after exceeding rate limit")
    void registrationBlockedAfterExceedingLimit() {
        String ip = "192.168.1.4";

        for (int i = 0; i < 3; i++) {
            rateLimitingService.isRegistrationAllowed(ip);
        }

        assertFalse(rateLimitingService.isRegistrationAllowed(ip),
                "4th registration attempt should be blocked");
    }

    @Test
    @DisplayName("Rate limits should be per IP address")
    void rateLimitsPerIpAddress() {
        String ip1 = "192.168.1.5";
        String ip2 = "192.168.1.6";

        for (int i = 0; i < 5; i++) {
            rateLimitingService.isLoginAllowed(ip1);
        }

        assertFalse(rateLimitingService.isLoginAllowed(ip1), "IP1 should be blocked");
        assertTrue(rateLimitingService.isLoginAllowed(ip2), "IP2 should still be allowed");
    }

    @Test
    @DisplayName("getRemainingLoginAttempts should return correct count")
    void getRemainingLoginAttemptsReturnsCorrectCount() {
        String ip = "192.168.1.7";

        assertEquals(5, rateLimitingService.getRemainingLoginAttempts(ip));

        rateLimitingService.isLoginAllowed(ip);
        assertEquals(4, rateLimitingService.getRemainingLoginAttempts(ip));

        rateLimitingService.isLoginAllowed(ip);
        assertEquals(3, rateLimitingService.getRemainingLoginAttempts(ip));
    }

    @Test
    @DisplayName("clearRateLimitForIp should reset rate limits")
    void clearRateLimitForIpResetsLimits() {
        String ip = "192.168.1.8";

        for (int i = 0; i < 5; i++) {
            rateLimitingService.isLoginAllowed(ip);
        }
        assertFalse(rateLimitingService.isLoginAllowed(ip), "Should be blocked");

        rateLimitingService.clearRateLimitForIp(ip);

        assertTrue(rateLimitingService.isLoginAllowed(ip), "Should be allowed after clear");
    }

    @Test
    @DisplayName("Password reset should be rate limited")
    void passwordResetRateLimited() {
        String ip = "192.168.1.9";

        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitingService.isPasswordResetAllowed(ip),
                    "Password reset attempt " + (i + 1) + " should be allowed");
        }

        assertFalse(rateLimitingService.isPasswordResetAllowed(ip),
                "4th password reset attempt should be blocked");
    }
}
