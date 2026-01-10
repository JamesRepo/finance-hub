package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.UserRegistrationDto;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserService Integration Tests")
class UserServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("User Registration")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register new user with valid data")
        void shouldRegisterNewUser() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("newuser@test.com");
            dto.setPassword("SecurePass@123");
            dto.setFirstName("John");
            dto.setLastName("Doe");

            User registeredUser = userService.registerUser(dto);

            assertNotNull(registeredUser.getUserId());
            assertEquals("newuser@test.com", registeredUser.getEmail());
            assertEquals("John", registeredUser.getFirstName());
            assertEquals("Doe", registeredUser.getLastName());
            assertTrue(passwordEncoder.matches("SecurePass@123", registeredUser.getPasswordHash()));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("TestUser@EXAMPLE.COM");
            dto.setPassword("SecurePass@123");
            dto.setFirstName("John");
            dto.setLastName("Doe");

            User registeredUser = userService.registerUser(dto);

            assertEquals("testuser@example.com", registeredUser.getEmail());
        }

        @Test
        @DisplayName("Should reject duplicate email (case-insensitive)")
        void shouldRejectDuplicateEmail() {
            UserRegistrationDto dto1 = new UserRegistrationDto();
            dto1.setEmail("duplicate@test.com");
            dto1.setPassword("SecurePass@123");
            dto1.setFirstName("First");
            dto1.setLastName("User");
            userService.registerUser(dto1);

            UserRegistrationDto dto2 = new UserRegistrationDto();
            dto2.setEmail("DUPLICATE@test.com");
            dto2.setPassword("SecurePass@456");
            dto2.setFirstName("Second");
            dto2.setLastName("User");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.registerUser(dto2)
            );
            assertEquals("Email already exists", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject password without uppercase")
        void shouldRejectPasswordWithoutUppercase() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("test@test.com");
            dto.setPassword("password@123");
            dto.setFirstName("Test");
            dto.setLastName("User");

            assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        }

        @Test
        @DisplayName("Should reject password without special character")
        void shouldRejectPasswordWithoutSpecialChar() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("test@test.com");
            dto.setPassword("Password123");
            dto.setFirstName("Test");
            dto.setLastName("User");

            assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        }

        @Test
        @DisplayName("Should reject short password")
        void shouldRejectShortPassword() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("test@test.com");
            dto.setPassword("Pa@1");
            dto.setFirstName("Test");
            dto.setLastName("User");

            assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        }
    }

    @Nested
    @DisplayName("Profile Update")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should update user profile")
        void shouldUpdateUserProfile() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("profile@test.com");
            dto.setPassword("SecurePass@123");
            dto.setFirstName("Original");
            dto.setLastName("Name");
            User user = userService.registerUser(dto);

            User updatedUser = userService.updateProfile(user, "Updated", "LastName", "profile@test.com");

            assertEquals("Updated", updatedUser.getFirstName());
            assertEquals("LastName", updatedUser.getLastName());
        }

        @Test
        @DisplayName("Should allow email change to new email")
        void shouldAllowEmailChangeToNewEmail() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("old@test.com");
            dto.setPassword("SecurePass@123");
            dto.setFirstName("Test");
            dto.setLastName("User");
            User user = userService.registerUser(dto);

            User updatedUser = userService.updateProfile(user, "Test", "User", "new@test.com");

            assertEquals("new@test.com", updatedUser.getEmail());
        }

        @Test
        @DisplayName("Should reject email change to existing email")
        void shouldRejectEmailChangeToExisting() {
            UserRegistrationDto dto1 = new UserRegistrationDto();
            dto1.setEmail("user1@test.com");
            dto1.setPassword("SecurePass@123");
            dto1.setFirstName("User");
            dto1.setLastName("One");
            userService.registerUser(dto1);

            UserRegistrationDto dto2 = new UserRegistrationDto();
            dto2.setEmail("user2@test.com");
            dto2.setPassword("SecurePass@123");
            dto2.setFirstName("User");
            dto2.setLastName("Two");
            User user2 = userService.registerUser(dto2);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateProfile(user2, "User", "Two", "user1@test.com"));
        }
    }

    @Nested
    @DisplayName("Password Change")
    class PasswordChangeTests {

        @Test
        @DisplayName("Should change password with correct current password")
        void shouldChangePassword() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("password@test.com");
            dto.setPassword("OldPass@123");
            dto.setFirstName("Test");
            dto.setLastName("User");
            User user = userService.registerUser(dto);

            userService.changePassword(user, "OldPass@123", "NewPass@456");

            User refreshedUser = userRepository.findById(user.getUserId()).orElseThrow();
            assertTrue(passwordEncoder.matches("NewPass@456", refreshedUser.getPasswordHash()));
            assertFalse(passwordEncoder.matches("OldPass@123", refreshedUser.getPasswordHash()));
        }

        @Test
        @DisplayName("Should reject password change with incorrect current password")
        void shouldRejectIncorrectCurrentPassword() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("wrongpass@test.com");
            dto.setPassword("CorrectPass@123");
            dto.setFirstName("Test");
            dto.setLastName("User");
            User user = userService.registerUser(dto);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.changePassword(user, "WrongPass@123", "NewPass@456")
            );
            assertEquals("Current password is incorrect", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject weak new password")
        void shouldRejectWeakNewPassword() {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setEmail("weaknew@test.com");
            dto.setPassword("StrongPass@123");
            dto.setFirstName("Test");
            dto.setLastName("User");
            User user = userService.registerUser(dto);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.changePassword(user, "StrongPass@123", "weak"));
        }
    }
}
