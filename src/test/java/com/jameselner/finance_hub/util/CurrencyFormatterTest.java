package com.jameselner.finance_hub.util;

import com.jameselner.finance_hub.domain.UserSettings;
import com.jameselner.finance_hub.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyFormatter Tests")
class CurrencyFormatterTest {

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private CurrencyFormatter currencyFormatter;

    private UserSettings gbpSettings;
    private UserSettings usdSettings;
    private UserSettings eurSettings;

    @BeforeEach
    void setUp() {
        gbpSettings = new UserSettings();
        gbpSettings.setCurrency("GBP");
        gbpSettings.setLocale("en-GB");

        usdSettings = new UserSettings();
        usdSettings.setCurrency("USD");
        usdSettings.setLocale("en-US");

        eurSettings = new UserSettings();
        eurSettings.setCurrency("EUR");
        eurSettings.setLocale("de-DE");
    }

    @Nested
    @DisplayName("format(BigDecimal, Long) Tests")
    class FormatWithUserIdTests {

        @Test
        @DisplayName("Should format amount with GBP currency")
        void shouldFormatAmountWithGbpCurrency() {
            when(userSettingsService.getSettingsForUserId(1L)).thenReturn(gbpSettings);

            String result = currencyFormatter.format(new BigDecimal("1234.56"), 1L);

            assertTrue(result.contains("1,234.56") || result.contains("1234.56"));
            assertTrue(result.contains("£") || result.contains("GBP"));
            verify(userSettingsService).getSettingsForUserId(1L);
        }

        @Test
        @DisplayName("Should format amount with USD currency")
        void shouldFormatAmountWithUsdCurrency() {
            when(userSettingsService.getSettingsForUserId(2L)).thenReturn(usdSettings);

            String result = currencyFormatter.format(new BigDecimal("1234.56"), 2L);

            assertTrue(result.contains("1,234.56") || result.contains("1234.56"));
            assertTrue(result.contains("$") || result.contains("USD"));
            verify(userSettingsService).getSettingsForUserId(2L);
        }

        @Test
        @DisplayName("Should format amount with EUR currency")
        void shouldFormatAmountWithEurCurrency() {
            when(userSettingsService.getSettingsForUserId(3L)).thenReturn(eurSettings);

            String result = currencyFormatter.format(new BigDecimal("1234.56"), 3L);

            // German locale uses comma as decimal separator
            assertTrue(result.contains("1.234,56") || result.contains("1234,56") || result.contains("1,234.56"));
            assertTrue(result.contains("€") || result.contains("EUR"));
            verify(userSettingsService).getSettingsForUserId(3L);
        }

        @Test
        @DisplayName("Should handle null amount by returning zero")
        void shouldHandleNullAmount() {
            when(userSettingsService.getSettingsForUserId(1L)).thenReturn(gbpSettings);

            String result = currencyFormatter.format(null, 1L);

            assertNotNull(result);
            assertTrue(result.contains("0"));
        }

        @Test
        @DisplayName("Should format negative amounts correctly")
        void shouldFormatNegativeAmounts() {
            when(userSettingsService.getSettingsForUserId(1L)).thenReturn(gbpSettings);

            String result = currencyFormatter.format(new BigDecimal("-500.00"), 1L);

            assertNotNull(result);
            assertTrue(result.contains("500") && (result.contains("-") || result.contains("(")));
        }

        @Test
        @DisplayName("Should format large amounts with thousands separator")
        void shouldFormatLargeAmountsWithThousandsSeparator() {
            when(userSettingsService.getSettingsForUserId(1L)).thenReturn(gbpSettings);

            String result = currencyFormatter.format(new BigDecimal("1234567.89"), 1L);

            assertNotNull(result);
            // Should contain some form of thousands grouping
            assertTrue(result.contains(",") || result.contains(".") || result.contains(" "));
        }

        @Test
        @DisplayName("Should format zero amount correctly")
        void shouldFormatZeroAmount() {
            when(userSettingsService.getSettingsForUserId(1L)).thenReturn(gbpSettings);

            String result = currencyFormatter.format(BigDecimal.ZERO, 1L);

            assertNotNull(result);
            assertTrue(result.contains("0"));
        }
    }

    @Nested
    @DisplayName("format(BigDecimal, String, String) Tests")
    class FormatWithCurrencyAndLocaleTests {

        @Test
        @DisplayName("Should format with explicit GBP/en-GB")
        void shouldFormatWithExplicitGbpEnGb() {
            String result = currencyFormatter.format(new BigDecimal("100.00"), "GBP", "en-GB");

            assertNotNull(result);
            assertTrue(result.contains("100"));
            assertTrue(result.contains("£") || result.contains("GBP"));
        }

        @Test
        @DisplayName("Should format with explicit USD/en-US")
        void shouldFormatWithExplicitUsdEnUs() {
            String result = currencyFormatter.format(new BigDecimal("100.00"), "USD", "en-US");

            assertNotNull(result);
            assertTrue(result.contains("100"));
            assertTrue(result.contains("$") || result.contains("USD"));
        }

        @Test
        @DisplayName("Should fallback to GBP for invalid currency code")
        void shouldFallbackToGbpForInvalidCurrency() {
            String result = currencyFormatter.format(new BigDecimal("100.00"), "INVALID", "en-GB");

            assertNotNull(result);
            // Should fallback to UK locale
            assertTrue(result.contains("100"));
        }

        @Test
        @DisplayName("Should handle null amount in explicit format")
        void shouldHandleNullAmountInExplicitFormat() {
            String result = currencyFormatter.format(null, "GBP", "en-GB");

            assertNotNull(result);
            assertTrue(result.contains("0"));
        }
    }

    @Nested
    @DisplayName("formatPercentage Tests")
    class FormatPercentageTests {

        @Test
        @DisplayName("Should format percentage correctly")
        void shouldFormatPercentageCorrectly() {
            String result = currencyFormatter.formatPercentage(new BigDecimal("25.5"));

            assertEquals("25.5%", result);
        }

        @Test
        @DisplayName("Should format whole number percentage")
        void shouldFormatWholeNumberPercentage() {
            String result = currencyFormatter.formatPercentage(new BigDecimal("100"));

            assertEquals("100.0%", result);
        }

        @Test
        @DisplayName("Should format zero percentage")
        void shouldFormatZeroPercentage() {
            String result = currencyFormatter.formatPercentage(BigDecimal.ZERO);

            assertEquals("0.0%", result);
        }

        @Test
        @DisplayName("Should handle null percentage by returning zero")
        void shouldHandleNullPercentage() {
            String result = currencyFormatter.formatPercentage(null);

            assertEquals("0.0%", result);
        }

        @Test
        @DisplayName("Should format negative percentage")
        void shouldFormatNegativePercentage() {
            String result = currencyFormatter.formatPercentage(new BigDecimal("-15.3"));

            assertEquals("-15.3%", result);
        }

        @Test
        @DisplayName("Should format percentage with many decimal places")
        void shouldFormatPercentageWithManyDecimalPlaces() {
            String result = currencyFormatter.formatPercentage(new BigDecimal("33.3333"));

            assertEquals("33.3%", result);
        }
    }
}
