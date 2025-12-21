package com.jameselner.finance_hub.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener for Spring Security authentication events.
 * Logs all authentication-related events for security monitoring and audit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventListener {

    private final SecurityAuditService securityAuditService;
    private final RateLimitingService rateLimitingService;

    /**
     * Handle successful authentication events.
     */
    @EventListener
    public void onAuthenticationSuccess(final AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIp();

        securityAuditService.logLoginSuccess(username, ipAddress);

        rateLimitingService.clearRateLimitForIp(ipAddress);
    }

    /**
     * Handle failed authentication events.
     */
    @EventListener
    public void onAuthenticationFailure(final AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIp();
        String reason = event.getException().getMessage();

        securityAuditService.logLoginFailure(username, ipAddress, reason);
    }

    /**
     * Handle logout events.
     */
    @EventListener
    public void onLogoutSuccess(final LogoutSuccessEvent event) {
        String username = event.getAuthentication() != null
                ? event.getAuthentication().getName()
                : "unknown";
        String ipAddress = getClientIp();

        securityAuditService.logLogout(username, ipAddress);
    }

    /**
     * Handle session created events.
     */
    @EventListener
    public void onSessionCreated(final HttpSessionCreatedEvent event) {
        String sessionId = event.getSession().getId();
        securityAuditService.logSessionCreated("unknown", getClientIp(), sessionId);
    }

    /**
     * Handle session destroyed events.
     */
    @EventListener
    public void onSessionDestroyed(final HttpSessionDestroyedEvent event) {
        String sessionId = event.getSession().getId();
        securityAuditService.logSessionDestroyed(sessionId);
    }

    /**
     * Extract client IP from current request context.
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Unable to get client IP: {}", e.getMessage());
        }
        return "unknown";
    }
}
