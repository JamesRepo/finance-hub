package com.jameselner.finance_hub.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    @DisplayName("Valid password should pass all checks")
    void validPasswordPasses() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("Test@123");
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Null password should fail")
    void nullPasswordFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate(null);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("required"));
    }

    @Test
    @DisplayName("Empty password should fail")
    void emptyPasswordFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("required"));
    }

    @Test
    @DisplayName("Short password should fail")
    void shortPasswordFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("Ab1!");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Password without uppercase should fail")
    void passwordWithoutUppercaseFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("test@123");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Password without lowercase should fail")
    void passwordWithoutLowercaseFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("TEST@123");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("lowercase"));
    }

    @Test
    @DisplayName("Password without digit should fail")
    void passwordWithoutDigitFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("Test@abc");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("number"));
    }

    @Test
    @DisplayName("Password without special character should fail")
    void passwordWithoutSpecialCharFails() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("Test1234");
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("special character"));
    }

    @ParameterizedTest
    @DisplayName("Valid complex passwords should pass")
    @ValueSource(strings = {
            "MyP@ssw0rd!",
            "Secure#Pass123",
            "C0mplex$Password",
            "Strong_Password1",
            "Test-Pass@2024"
    })
    void validComplexPasswordsPasses(String password) {
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        assertTrue(result.isValid(), "Password should be valid: " + password);
    }

    @ParameterizedTest
    @DisplayName("Weak passwords should fail")
    @ValueSource(strings = {
            "password",
            "12345678",
            "ALLUPPERCASE",
            "alllowercase",
            "NoSpecial1",
            "no-digit!"
    })
    void weakPasswordsFail(String password) {
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        assertFalse(result.isValid(), "Password should be invalid: " + password);
    }

    @Test
    @DisplayName("Multiple errors should be combined in message")
    void multipleErrorsCombined() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("short");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 1);
        String message = result.getErrorMessage();
        assertNotNull(message);
        assertTrue(message.contains(". "));
    }

    @Test
    @DisplayName("getRequirementsText should return requirements description")
    void getRequirementsTextReturnsDescription() {
        String requirements = PasswordValidator.getRequirementsText();
        assertNotNull(requirements);
        assertTrue(requirements.contains("8 characters"));
        assertTrue(requirements.contains("uppercase"));
        assertTrue(requirements.contains("lowercase"));
        assertTrue(requirements.contains("number"));
        assertTrue(requirements.contains("special character"));
    }
}
