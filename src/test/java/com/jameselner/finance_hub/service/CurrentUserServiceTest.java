package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentUserService Tests")
class CurrentUserServiceTest {

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private CurrentUserService currentUserService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Nested
    @DisplayName("getCurrentUser Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user when authenticated")
        void shouldReturnCurrentUserWhenAuthenticated() {
            when(userDetails.getUsername()).thenReturn("test@example.com");
            when(authenticationContext.getAuthenticatedUser(UserDetails.class))
                    .thenReturn(Optional.of(userDetails));
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            User result = currentUserService.getCurrentUser();

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals("test@example.com", result.getEmail());
            verify(authenticationContext).getAuthenticatedUser(UserDetails.class);
            verify(userRepository).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            when(authenticationContext.getAuthenticatedUser(UserDetails.class))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> currentUserService.getCurrentUser()
            );

            assertEquals("User not found", exception.getMessage());
            verify(authenticationContext).getAuthenticatedUser(UserDetails.class);
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw exception when user not found in database")
        void shouldThrowExceptionWhenUserNotFoundInDatabase() {
            when(userDetails.getUsername()).thenReturn("nonexistent@example.com");
            when(authenticationContext.getAuthenticatedUser(UserDetails.class))
                    .thenReturn(Optional.of(userDetails));
            when(userRepository.findByEmail("nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> currentUserService.getCurrentUser()
            );

            assertEquals("User not found", exception.getMessage());
            verify(authenticationContext).getAuthenticatedUser(UserDetails.class);
            verify(userRepository).findByEmail("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("getCurrentUserId Tests")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("Should return current user ID when authenticated")
        void shouldReturnCurrentUserIdWhenAuthenticated() {
            when(userDetails.getUsername()).thenReturn("test@example.com");
            when(authenticationContext.getAuthenticatedUser(UserDetails.class))
                    .thenReturn(Optional.of(userDetails));
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            Long result = currentUserService.getCurrentUserId();

            assertEquals(1L, result);
        }

        @Test
        @DisplayName("Should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            when(authenticationContext.getAuthenticatedUser(UserDetails.class))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> currentUserService.getCurrentUserId());
        }
    }
}
