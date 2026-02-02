package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.UserSettings;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSettingsService Tests")
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private User testUser;
    private UserSettings testSettings;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        testSettings = new UserSettings();
        testSettings.setSettingsId(1L);
        testSettings.setUser(testUser);
        testSettings.setCurrency("GBP");
        testSettings.setLocale("en-GB");
    }

    @Nested
    @DisplayName("getSettingsForUser Tests")
    class GetSettingsForUserTests {

        @Test
        @DisplayName("Should return existing settings for user")
        void shouldReturnExistingSettingsForUser() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.of(testSettings));

            UserSettings result = userSettingsService.getSettingsForUser(testUser);

            assertNotNull(result);
            assertEquals("GBP", result.getCurrency());
            assertEquals("en-GB", result.getLocale());
            verify(userSettingsRepository).findByUserUserId(1L);
            verify(userSettingsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create default settings when none exist")
        void shouldCreateDefaultSettingsWhenNoneExist() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.empty());
            when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> {
                UserSettings settings = invocation.getArgument(0);
                settings.setSettingsId(2L);
                return settings;
            });

            UserSettings result = userSettingsService.getSettingsForUser(testUser);

            assertNotNull(result);
            assertEquals("GBP", result.getCurrency());
            assertEquals("en-GB", result.getLocale());
            assertEquals(testUser, result.getUser());
            verify(userSettingsRepository).findByUserUserId(1L);
            verify(userSettingsRepository).save(any(UserSettings.class));
        }
    }

    @Nested
    @DisplayName("getSettingsForUserId Tests")
    class GetSettingsForUserIdTests {

        @Test
        @DisplayName("Should return existing settings for user ID")
        void shouldReturnExistingSettingsForUserId() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.of(testSettings));

            UserSettings result = userSettingsService.getSettingsForUserId(1L);

            assertNotNull(result);
            assertEquals("GBP", result.getCurrency());
            assertEquals("en-GB", result.getLocale());
            verify(userSettingsRepository).findByUserUserId(1L);
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should create default settings when none exist for user ID")
        void shouldCreateDefaultSettingsWhenNoneExistForUserId() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> {
                UserSettings settings = invocation.getArgument(0);
                settings.setSettingsId(2L);
                return settings;
            });

            UserSettings result = userSettingsService.getSettingsForUserId(1L);

            assertNotNull(result);
            assertEquals("GBP", result.getCurrency());
            assertEquals("en-GB", result.getLocale());
            verify(userSettingsRepository).findByUserUserId(1L);
            verify(userRepository).findById(1L);
            verify(userSettingsRepository).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found for user ID")
        void shouldThrowExceptionWhenUserNotFoundForUserId() {
            when(userSettingsRepository.findByUserUserId(999L)).thenReturn(Optional.empty());
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userSettingsService.getSettingsForUserId(999L)
            );

            assertEquals("User not found: 999", exception.getMessage());
            verify(userSettingsRepository).findByUserUserId(999L);
            verify(userRepository).findById(999L);
            verify(userSettingsRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createDefaultSettings Tests")
    class CreateDefaultSettingsTests {

        @Test
        @DisplayName("Should create settings with default GBP currency")
        void shouldCreateSettingsWithDefaultGbpCurrency() {
            when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> {
                UserSettings settings = invocation.getArgument(0);
                settings.setSettingsId(1L);
                return settings;
            });

            UserSettings result = userSettingsService.createDefaultSettings(testUser);

            assertNotNull(result);
            assertEquals("GBP", result.getCurrency());
            assertEquals(testUser, result.getUser());

            ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
            verify(userSettingsRepository).save(captor.capture());

            UserSettings saved = captor.getValue();
            assertEquals("GBP", saved.getCurrency());
            assertEquals("en-GB", saved.getLocale());
        }

        @Test
        @DisplayName("Should create settings with default en-GB locale")
        void shouldCreateSettingsWithDefaultEnGbLocale() {
            when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> {
                UserSettings settings = invocation.getArgument(0);
                settings.setSettingsId(1L);
                return settings;
            });

            UserSettings result = userSettingsService.createDefaultSettings(testUser);

            assertEquals("en-GB", result.getLocale());
        }
    }

    @Nested
    @DisplayName("updateCurrency Tests")
    class UpdateCurrencyTests {

        @Test
        @DisplayName("Should update currency for existing settings")
        void shouldUpdateCurrencyForExistingSettings() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.of(testSettings));
            when(userSettingsRepository.save(testSettings)).thenReturn(testSettings);

            userSettingsService.updateCurrency(testUser, "USD");

            assertEquals("USD", testSettings.getCurrency());
            verify(userSettingsRepository).findByUserUserId(1L);
            verify(userSettingsRepository).save(testSettings);
        }

        @Test
        @DisplayName("Should create settings and update currency when none exist")
        void shouldCreateSettingsAndUpdateCurrencyWhenNoneExist() {
            when(userSettingsRepository.findByUserUserId(1L)).thenReturn(Optional.empty());
            when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> {
                UserSettings settings = invocation.getArgument(0);
                settings.setSettingsId(2L);
                return settings;
            });

            userSettingsService.updateCurrency(testUser, "EUR");

            // First call creates default, second call saves with EUR
            verify(userSettingsRepository, times(2)).save(any(UserSettings.class));
        }
    }
}
