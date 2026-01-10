package com.jameselner.finance_hub.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS and injection attacks.
 * Provides defense-in-depth alongside Vaadin's built-in escaping.
 */
@Component
public class InputSanitizer {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
            "<script",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "\\s*on\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "javascript\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATA_URI_PATTERN = Pattern.compile(
            "data\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize a string by removing potentially dangerous HTML/JavaScript content.
     * Use this for user-provided text that will be displayed.
     *
     * @param input the input string to sanitize
     * @return the sanitized string, or null if input was null
     */
    public String sanitize(final String input) {
        if (input == null) {
            return null;
        }

        String result = input;

        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll(" ");
        result = JAVASCRIPT_PATTERN.matcher(result).replaceAll("");
        result = DATA_URI_PATTERN.matcher(result).replaceAll("");

        return result.trim();
    }

    /**
     * Sanitize a string and also escape HTML entities for safe display.
     *
     * @param input the input string to sanitize and escape
     * @return the sanitized and escaped string
     */
    public String sanitizeAndEscape(final String input) {
        if (input == null) {
            return null;
        }

        String sanitized = sanitize(input);
        return escapeHtml(sanitized);
    }

    /**
     * Escape HTML special characters to prevent XSS.
     *
     * @param input the input string to escape
     * @return the escaped string
     */
    public String escapeHtml(final String input) {
        if (input == null) {
            return null;
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Check if a string contains potentially dangerous content.
     *
     * @param input the input string to check
     * @return true if the string contains suspicious content
     */
    public boolean containsSuspiciousContent(final String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return SCRIPT_TAG_PATTERN.matcher(input).find()
                || EVENT_HANDLER_PATTERN.matcher(input).find()
                || JAVASCRIPT_PATTERN.matcher(input).find();
    }

    /**
     * Validate that a string contains only alphanumeric characters and common punctuation.
     * Useful for names and simple text fields.
     *
     * @param input the input string to validate
     * @return true if the string is safe
     */
    public boolean isValidSimpleText(final String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        return input.matches("^[\\p{L}\\p{N} .,'-]+$");
    }
}
