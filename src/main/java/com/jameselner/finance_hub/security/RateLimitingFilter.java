package com.jameselner.finance_hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that enforces rate limiting on authentication endpoints.
 * Protects against brute force attacks by limiting login and registration attempts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";
    private static final String REGISTER_PATH = "/register";

    private final RateLimitingService rateLimitingService;
    private final SecurityAuditService securityAuditService;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        if (isLoginAttempt(path, method)) {
            if (!rateLimitingService.isLoginAllowed(clientIp)) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp);
                securityAuditService.logRateLimitExceeded(clientIp, "LOGIN");
                handleRateLimitExceeded(response, "login");
                return;
            }
        } else if (isRegistrationAttempt(path, method)) {
            if (!rateLimitingService.isRegistrationAllowed(clientIp)) {
                log.warn("Rate limit exceeded for registration from IP: {}", clientIp);
                securityAuditService.logRateLimitExceeded(clientIp, "REGISTRATION");
                handleRateLimitExceeded(response, "registration");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginAttempt(final String path, final String method) {
        return LOGIN_PATH.equals(path) && "POST".equalsIgnoreCase(method);
    }

    private boolean isRegistrationAttempt(final String path, final String method) {
        return REGISTER_PATH.equals(path) && "POST".equalsIgnoreCase(method);
    }

    private void handleRateLimitExceeded(final HttpServletResponse response, final String action) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<html><body>" +
                "<h1>Too Many Requests</h1>" +
                "<p>Too many " + action + " attempts. Please try again later.</p>" +
                "</body></html>"
        );
    }

    /**
     * Extract the client IP address from the request.
     * Handles X-Forwarded-For header for requests behind proxies/load balancers.
     */
    private String getClientIp(final HttpServletRequest request) {
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
}
