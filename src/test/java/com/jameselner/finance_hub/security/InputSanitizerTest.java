package com.jameselner.finance_hub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    @Test
    @DisplayName("Sanitize should handle null input")
    void sanitizeHandlesNull() {
        assertNull(inputSanitizer.sanitize(null));
    }

    @Test
    @DisplayName("Sanitize should handle empty input")
    void sanitizeHandlesEmpty() {
        assertEquals("", inputSanitizer.sanitize(""));
    }

    @Test
    @DisplayName("Sanitize should preserve normal text")
    void sanitizePreservesNormalText() {
        String input = "Hello, World! This is normal text.";
        assertEquals(input, inputSanitizer.sanitize(input));
    }

    @Test
    @DisplayName("Sanitize should remove script tags")
    void sanitizeRemovesScriptTags() {
        String input = "Hello<script>alert('xss')</script>World";
        assertEquals("HelloWorld", inputSanitizer.sanitize(input));
    }

    @Test
    @DisplayName("Sanitize should remove script tags case insensitively")
    void sanitizeRemovesScriptTagsCaseInsensitive() {
        String input = "Hello<SCRIPT>alert('xss')</SCRIPT>World";
        assertEquals("HelloWorld", inputSanitizer.sanitize(input));
    }

    @Test
    @DisplayName("Sanitize should remove event handlers")
    void sanitizeRemovesEventHandlers() {
        String input = "Hello onclick=alert('xss') World";
        String result = inputSanitizer.sanitize(input);
        assertFalse(result.contains("onclick"));
    }

    @ParameterizedTest
    @DisplayName("Sanitize should remove various event handlers")
    @ValueSource(strings = {
            "onload=",
            "onerror=",
            "onmouseover=",
            "onfocus=",
            "onchange="
    })
    void sanitizeRemovesVariousEventHandlers(String eventHandler) {
        String input = "Hello " + eventHandler + "alert('xss') World";
        String result = inputSanitizer.sanitize(input);
        assertFalse(result.toLowerCase().contains(eventHandler.toLowerCase()));
    }

    @Test
    @DisplayName("Sanitize should remove javascript: protocol")
    void sanitizeRemovesJavascriptProtocol() {
        String input = "Click here javascript:alert('xss')";
        String result = inputSanitizer.sanitize(input);
        assertFalse(result.contains("javascript"));
    }

    @Test
    @DisplayName("Sanitize should remove data: protocol")
    void sanitizeRemovesDataProtocol() {
        String input = "Image data:text/html,<script>alert('xss')</script>";
        String result = inputSanitizer.sanitize(input);
        assertFalse(result.contains("data:"));
    }

    @Test
    @DisplayName("escapeHtml should escape special characters")
    void escapeHtmlEscapesSpecialCharacters() {
        String input = "<script>alert('test' & \"hello\")</script>";
        String result = inputSanitizer.escapeHtml(input);

        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&quot;"));
        assertTrue(result.contains("&#x27;"));
    }

    @Test
    @DisplayName("containsSuspiciousContent should detect script tags")
    void containsSuspiciousContentDetectsScriptTags() {
        assertTrue(inputSanitizer.containsSuspiciousContent("<script>alert('xss')</script>"));
    }

    @Test
    @DisplayName("containsSuspiciousContent should detect event handlers")
    void containsSuspiciousContentDetectsEventHandlers() {
        assertTrue(inputSanitizer.containsSuspiciousContent("Hello onclick=alert('xss')"));
    }

    @Test
    @DisplayName("containsSuspiciousContent should detect javascript protocol")
    void containsSuspiciousContentDetectsJavascriptProtocol() {
        assertTrue(inputSanitizer.containsSuspiciousContent("javascript:alert('xss')"));
    }

    @Test
    @DisplayName("containsSuspiciousContent should return false for safe content")
    void containsSuspiciousContentReturnsFalseForSafeContent() {
        assertFalse(inputSanitizer.containsSuspiciousContent("Hello, World!"));
        assertFalse(inputSanitizer.containsSuspiciousContent("This is safe text."));
    }

    @Test
    @DisplayName("isValidSimpleText should accept valid names")
    void isValidSimpleTextAcceptsValidNames() {
        assertTrue(inputSanitizer.isValidSimpleText("John"));
        assertTrue(inputSanitizer.isValidSimpleText("Mary Jane"));
        assertTrue(inputSanitizer.isValidSimpleText("O'Connor"));
        assertTrue(inputSanitizer.isValidSimpleText("Jean-Pierre"));
    }

    @Test
    @DisplayName("isValidSimpleText should reject suspicious content")
    void isValidSimpleTextRejectsSuspiciousContent() {
        assertFalse(inputSanitizer.isValidSimpleText("<script>"));
        assertFalse(inputSanitizer.isValidSimpleText("Hello!@#$%"));
    }

    @Test
    @DisplayName("sanitizeAndEscape should both sanitize and escape")
    void sanitizeAndEscapeCombinesBoth() {
        String input = "<script>alert('xss')</script><b>Bold</b>";
        String result = inputSanitizer.sanitizeAndEscape(input);

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;b&gt;"));
    }
}
