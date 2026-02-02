package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving the currently authenticated user.
 * Centralizes user retrieval logic that was previously duplicated across views.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user.
     *
     * @return the current User entity
     * @throws RuntimeException if no user is authenticated or user not found
     */
    public User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get the ID of the currently authenticated user.
     *
     * @return the current user's ID
     * @throws RuntimeException if no user is authenticated or user not found
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
