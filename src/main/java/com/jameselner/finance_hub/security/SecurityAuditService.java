package com.jameselner.finance_hub.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Service for logging security-related events for audit and monitoring purposes.
 * All security events are logged with structured information for analysis.
 */
@Slf4j
@Service
public class SecurityAuditService {

    private static final String SECURITY_MARKER = "[SECURITY_AUDIT]";

    /**
     * Log a successful login event.
     */
    public void logLoginSuccess(final String email, final String ipAddress) {
        log.info("{} LOGIN_SUCCESS - User: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, OffsetDateTime.now());
    }

    /**
     * Log a failed login attempt.
     */
    public void logLoginFailure(final String email, final String ipAddress, final String reason) {
        log.warn("{} LOGIN_FAILURE - User: {}, IP: {}, Reason: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, reason, OffsetDateTime.now());
    }

    /**
     * Log a user logout event.
     */
    public void logLogout(final String email, final String ipAddress) {
        log.info("{} LOGOUT - User: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, OffsetDateTime.now());
    }

    /**
     * Log a successful registration event.
     */
    public void logRegistrationSuccess(final String email, final String ipAddress) {
        log.info("{} REGISTRATION_SUCCESS - User: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, OffsetDateTime.now());
    }

    /**
     * Log a failed registration attempt.
     */
    public void logRegistrationFailure(final String email, final String ipAddress, final String reason) {
        log.warn("{} REGISTRATION_FAILURE - User: {}, IP: {}, Reason: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, reason, OffsetDateTime.now());
    }

    /**
     * Log a password change event.
     */
    public void logPasswordChange(final String email, final String ipAddress) {
        log.info("{} PASSWORD_CHANGE - User: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, OffsetDateTime.now());
    }

    /**
     * Log a failed password change attempt.
     */
    public void logPasswordChangeFailure(final String email, final String ipAddress, final String reason) {
        log.warn("{} PASSWORD_CHANGE_FAILURE - User: {}, IP: {}, Reason: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, reason, OffsetDateTime.now());
    }

    /**
     * Log a rate limit exceeded event.
     */
    public void logRateLimitExceeded(final String ipAddress, final String action) {
        log.warn("{} RATE_LIMIT_EXCEEDED - Action: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, action, ipAddress, OffsetDateTime.now());
    }

    /**
     * Log an access denied event.
     */
    public void logAccessDenied(final String email, final String ipAddress, final String resource) {
        log.warn("{} ACCESS_DENIED - User: {}, IP: {}, Resource: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, resource, OffsetDateTime.now());
    }

    /**
     * Log a session created event.
     */
    public void logSessionCreated(final String email, final String ipAddress, final String sessionId) {
        log.info("{} SESSION_CREATED - User: {}, IP: {}, SessionId: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, maskSessionId(sessionId), OffsetDateTime.now());
    }

    /**
     * Log a session destroyed event.
     */
    public void logSessionDestroyed(final String sessionId) {
        log.info("{} SESSION_DESTROYED - SessionId: {}, Timestamp: {}",
                SECURITY_MARKER, maskSessionId(sessionId), OffsetDateTime.now());
    }

    /**
     * Log a suspicious activity event.
     */
    public void logSuspiciousActivity(final String ipAddress, final String description) {
        log.warn("{} SUSPICIOUS_ACTIVITY - IP: {}, Description: {}, Timestamp: {}",
                SECURITY_MARKER, ipAddress, description, OffsetDateTime.now());
    }

    /**
     * Log a profile update event.
     */
    public void logProfileUpdate(final String email, final String ipAddress) {
        log.info("{} PROFILE_UPDATE - User: {}, IP: {}, Timestamp: {}",
                SECURITY_MARKER, maskEmail(email), ipAddress, OffsetDateTime.now());
    }

    /**
     * Mask email address for logging (show first 3 chars + domain).
     */
    private String maskEmail(final String email) {
        if (email == null || email.length() < 4) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "**" + domain;
        }
        return localPart.substring(0, 3) + "***" + domain;
    }

    /**
     * Mask session ID for logging (show first 8 chars).
     */
    private String maskSessionId(final String sessionId) {
        if (sessionId == null || sessionId.length() < 8) {
            return "***";
        }
        return sessionId.substring(0, 8) + "...";
    }
}
