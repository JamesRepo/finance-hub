package com.jameselner.finance_hub.integration.security;

import com.jameselner.finance_hub.integration.BaseIntegrationTest;
import com.jameselner.finance_hub.security.InputSanitizer;
import com.jameselner.finance_hub.security.PasswordValidator;
import com.jameselner.finance_hub.security.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security Services Integration Tests")
class SecurityServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow initial login attempts")
        void shouldAllowInitialLoginAttempts() {
            String ip = "192.168.1.100";
            assertTrue(rateLimitingService.isLoginAllowed(ip));
        }

        @Test
        @DisplayName("Should rate limit after exceeding login attempts")
        void shouldRateLimitAfterExceedingLoginAttempts() {
            String ip = "192.168.1.101";

            // First 5 attempts should succeed
            for (int i = 0; i < 5; i++) {
                assertTrue(rateLimitingService.isLoginAllowed(ip),
                        "Attempt " + (i + 1) + " should be allowed");
            }

            // 6th attempt should be blocked
            assertFalse(rateLimitingService.isLoginAllowed(ip),
                    "6th attempt should be blocked");
        }

        @Test
        @DisplayName("Should rate limit registration attempts")
        void shouldRateLimitRegistrationAttempts() {
            String ip = "192.168.1.102";

            // First 3 attempts should succeed
            for (int i = 0; i < 3; i++) {
                assertTrue(rateLimitingService.isRegistrationAllowed(ip),
                        "Attempt " + (i + 1) + " should be allowed");
            }

            // 4th attempt should be blocked
            assertFalse(rateLimitingService.isRegistrationAllowed(ip),
                    "4th attempt should be blocked");
        }

        @Test
        @DisplayName("Should isolate rate limits per IP")
        void shouldIsolateRateLimitsPerIp() {
            String ip1 = "192.168.1.103";
            String ip2 = "192.168.1.104";

            // Exhaust rate limit for ip1
            for (int i = 0; i < 5; i++) {
                rateLimitingService.isLoginAllowed(ip1);
            }
            assertFalse(rateLimitingService.isLoginAllowed(ip1));

            // ip2 should still be allowed
            assertTrue(rateLimitingService.isLoginAllowed(ip2));
        }

        @Test
        @DisplayName("Should clear rate limit for IP on success")
        void shouldClearRateLimitForIp() {
            String ip = "192.168.1.105";

            // Exhaust rate limit
            for (int i = 0; i < 5; i++) {
                rateLimitingService.isLoginAllowed(ip);
            }
            assertFalse(rateLimitingService.isLoginAllowed(ip));

            // Clear rate limit (simulating successful login)
            rateLimitingService.clearRateLimitForIp(ip);

            // Should be allowed again
            assertTrue(rateLimitingService.isLoginAllowed(ip));
        }

        @Test
        @DisplayName("Should handle separate limits for login and registration")
        void shouldHandleSeparateLimitsForLoginAndRegistration() {
            String ip = "192.168.1.106";

            // Exhaust login limit
            for (int i = 0; i < 5; i++) {
                rateLimitingService.isLoginAllowed(ip);
            }

            // Registration should still work (separate bucket)
            assertTrue(rateLimitingService.isRegistrationAllowed(ip));
        }
    }

    @Nested
    @DisplayName("Password Validation")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should accept strong password")
        void shouldAcceptStrongPassword() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("SecurePass@123");
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Should reject password without uppercase")
        void shouldRejectPasswordWithoutUppercase() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("securepass@123");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("uppercase"));
        }

        @Test
        @DisplayName("Should reject password without lowercase")
        void shouldRejectPasswordWithoutLowercase() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("SECUREPASS@123");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("lowercase"));
        }

        @Test
        @DisplayName("Should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("SecurePass@abc");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("number"));
        }

        @Test
        @DisplayName("Should reject password without special character")
        void shouldRejectPasswordWithoutSpecialChar() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("SecurePass123");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("special"));
        }

        @Test
        @DisplayName("Should reject short password")
        void shouldRejectShortPassword() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("Aa@1");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("8 characters"));
        }

        @Test
        @DisplayName("Should accept password with various special characters")
        void shouldAcceptPasswordWithVariousSpecialChars() {
            String[] validPasswords = {
                    "Password!123",
                    "Password@123",
                    "Password#123",
                    "Password$123",
                    "Password%123",
                    "Password^123",
                    "Password&123",
                    "Password*123"
            };

            for (String password : validPasswords) {
                PasswordValidator.ValidationResult result =
                        PasswordValidator.validate(password);
                assertTrue(result.isValid(), "Password '" + password + "' should be valid");
            }
        }

        @Test
        @DisplayName("Should collect multiple errors")
        void shouldCollectMultipleErrors() {
            PasswordValidator.ValidationResult result =
                    PasswordValidator.validate("short");
            assertFalse(result.isValid());
            assertTrue(result.getErrors().size() > 1);
        }
    }

    @Nested
    @DisplayName("Input Sanitization")
    class InputSanitizationTests {

        @Test
        @DisplayName("Should sanitize script tags")
        void shouldSanitizeScriptTags() {
            String malicious = "<script>alert('xss')</script>";
            String sanitized = inputSanitizer.sanitize(malicious);
            assertFalse(sanitized.contains("<script>"));
            assertFalse(sanitized.contains("</script>"));
        }

        @Test
        @DisplayName("Should sanitize event handlers")
        void shouldSanitizeEventHandlers() {
            String malicious = "<img src='x' onerror='alert(1)'>";
            String sanitized = inputSanitizer.sanitize(malicious);
            assertFalse(sanitized.contains("onerror"));
        }

        @Test
        @DisplayName("Should sanitize javascript protocol")
        void shouldSanitizeJavascriptProtocol() {
            String malicious = "<a href='javascript:alert(1)'>Click</a>";
            String sanitized = inputSanitizer.sanitize(malicious);
            assertFalse(sanitized.contains("javascript:"));
        }

        @Test
        @DisplayName("Should preserve normal text")
        void shouldPreserveNormalText() {
            String normal = "This is normal text with numbers 123 and punctuation!";
            String sanitized = inputSanitizer.sanitize(normal);
            assertEquals(normal, sanitized);
        }

        @Test
        @DisplayName("Should detect suspicious content")
        void shouldDetectSuspiciousContent() {
            assertTrue(inputSanitizer.containsSuspiciousContent("<script>"));
            assertTrue(inputSanitizer.containsSuspiciousContent("onclick="));
            assertTrue(inputSanitizer.containsSuspiciousContent("javascript:"));
            assertFalse(inputSanitizer.containsSuspiciousContent("normal text"));
        }

        @Test
        @DisplayName("Should escape HTML entities")
        void shouldEscapeHtmlEntities() {
            String input = "<div>Test & \"quoted\"</div>";
            String escaped = inputSanitizer.escapeHtml(input);
            assertTrue(escaped.contains("&lt;"));
            assertTrue(escaped.contains("&gt;"));
            assertTrue(escaped.contains("&amp;"));
            assertTrue(escaped.contains("&quot;"));
        }

        @Test
        @DisplayName("Should validate simple text")
        void shouldValidateSimpleText() {
            assertTrue(inputSanitizer.isValidSimpleText("John Doe"));
            assertTrue(inputSanitizer.isValidSimpleText("O'Connor"));
            assertTrue(inputSanitizer.isValidSimpleText("Test-Name"));
            assertFalse(inputSanitizer.isValidSimpleText("<script>"));
            assertFalse(inputSanitizer.isValidSimpleText("Test\nName"));
        }

        @Test
        @DisplayName("Should handle null input safely")
        void shouldHandleNullInputSafely() {
            assertNull(inputSanitizer.sanitize(null));
            assertNull(inputSanitizer.escapeHtml(null));
            assertFalse(inputSanitizer.containsSuspiciousContent(null));
        }

        @Test
        @DisplayName("Should handle empty input safely")
        void shouldHandleEmptyInputSafely() {
            assertEquals("", inputSanitizer.sanitize(""));
            assertEquals("", inputSanitizer.escapeHtml(""));
            assertFalse(inputSanitizer.containsSuspiciousContent(""));
        }
    }
}
