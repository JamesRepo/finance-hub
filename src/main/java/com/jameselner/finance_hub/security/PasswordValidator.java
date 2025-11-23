package com.jameselner.finance_hub.security;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    public static ValidationResult validate(final String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return new ValidationResult(false, errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (!password.matches(UPPERCASE_PATTERN)) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!password.matches(LOWERCASE_PATTERN)) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!password.matches(DIGIT_PATTERN)) {
            errors.add("Password must contain at least one number");
        }

        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':\"\\|,.<>/?)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static String getRequirementsText() {
        return "Password must be at least " + MIN_LENGTH + " characters and contain: " +
               "uppercase letter, lowercase letter, number, and special character";
    }

    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(final boolean valid, final List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public String getErrorMessage() {
            return String.join(". ", errors);
        }
    }
}
