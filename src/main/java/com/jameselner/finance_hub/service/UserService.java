package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.dto.UserRegistrationDto;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.security.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        return userRepository.save(user);
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

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(final User user, final String currentPassword, final String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        PasswordValidator.ValidationResult passwordValidation =
            PasswordValidator.validate(newPassword);
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(passwordValidation.getErrorMessage());
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
