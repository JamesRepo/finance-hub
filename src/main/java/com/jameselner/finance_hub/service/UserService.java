package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.dto.UserRegistrationDto;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.security.PasswordValidator;
import com.jameselner.finance_hub.security.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    @Transactional
    public User registerUser(final UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail().toLowerCase())) {
            throw new IllegalArgumentException("Email already exists");
        }

        PasswordValidator.ValidationResult passwordValidation =
            PasswordValidator.validate(registrationDto.getPassword());
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(passwordValidation.getErrorMessage());
        }

        User user = new User();
        user.setEmail(registrationDto.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());

        User savedUser = userRepository.save(user);
        securityAuditService.logRegistrationSuccess(savedUser.getEmail(), getClientIp());
        return savedUser;
    }

    @Transactional
    public User updateProfile(final User user, final String firstName, final String lastName, final String email) {
        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmail(email.toLowerCase())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.toLowerCase());

        User savedUser = userRepository.save(user);
        securityAuditService.logProfileUpdate(savedUser.getEmail(), getClientIp());
        return savedUser;
    }

    @Transactional
    public void changePassword(final User user, final String currentPassword, final String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            securityAuditService.logPasswordChangeFailure(user.getEmail(), getClientIp(), "Invalid current password");
            throw new IllegalArgumentException("Current password is incorrect");
        }

        PasswordValidator.ValidationResult passwordValidation =
            PasswordValidator.validate(newPassword);
        if (!passwordValidation.isValid()) {
            securityAuditService.logPasswordChangeFailure(user.getEmail(), getClientIp(), "Invalid new password");
            throw new IllegalArgumentException(passwordValidation.getErrorMessage());
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        securityAuditService.logPasswordChange(user.getEmail(), getClientIp());
    }

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
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown";
    }
}
