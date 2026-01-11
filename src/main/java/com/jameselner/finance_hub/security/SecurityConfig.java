package com.jameselner.finance_hub.security;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.jameselner.finance_hub.view.LoginView;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@Configuration
@EnableWebSecurity
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        // Configure authorization rules
        http.authorizeHttpRequests(auth ->
                auth.requestMatchers("/register", "/images/**").permitAll()
        );

        // Configure security headers
        http.headers(headers -> headers
                // Prevent clickjacking attacks
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                // Prevent MIME type sniffing - adds X-Content-Type-Options: nosniff
                .contentTypeOptions(contentTypeOptions -> {})
                // Add Referrer-Policy header
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                // Add Content Security Policy
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data: blob:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'"
                        )
                )
                // Enable HSTS (HTTP Strict Transport Security)
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000) // 1 year
                )
                // Add Permissions-Policy header to restrict browser features
                .permissionsPolicy(permissions -> permissions
                        .policy("geolocation=(), microphone=(), camera=(), payment=(self)")
                )
        )

        // Add cache control for sensitive pages
        .headers(headers -> headers
                .cacheControl(cache -> {})
        );

        // Configure session management
        http.sessionManagement(session -> session
                // Protect against session fixation attacks
                .sessionFixation().newSession()
                // Set maximum sessions per user
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
        );

        // Apply Vaadin-specific security configuration
        http.with(vaadin(), vaadinConfig ->
                vaadinConfig.loginView(LoginView.class, "/")
        );

        return http.build();
    }
}
