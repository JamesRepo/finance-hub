package com.jameselner.finance_hub.util;

import com.jameselner.finance_hub.domain.UserSettings;
import com.jameselner.finance_hub.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Utility for formatting currency values according to user preferences.
 * Uses the currency and locale from UserSettings.
 */
@Component
@RequiredArgsConstructor
public class CurrencyFormatter {

    private final UserSettingsService userSettingsService;

    /**
     * Format a BigDecimal amount as currency using the user's preferred settings.
     *
     * @param amount the amount to format (null-safe, defaults to zero)
     * @param userId the user whose settings to use
     * @return formatted currency string
     */
    public String format(final BigDecimal amount, final Long userId) {
        UserSettings settings = userSettingsService.getSettingsForUserId(userId);
        return format(amount, settings.getCurrency(), settings.getLocale());
    }

    /**
     * Format a BigDecimal amount as currency using specified currency and locale.
     *
     * @param amount the amount to format (null-safe, defaults to zero)
     * @param currencyCode ISO 4217 currency code (e.g., "GBP", "USD")
     * @param localeTag IETF BCP 47 language tag (e.g., "en-GB", "en-US")
     * @return formatted currency string
     */
    public String format(final BigDecimal amount, final String currencyCode, final String localeTag) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;

        try {
            Currency currency = Currency.getInstance(currencyCode);
            Locale locale = Locale.forLanguageTag(localeTag);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
            formatter.setCurrency(currency);
            return formatter.format(safeAmount);
        } catch (IllegalArgumentException e) {
            // Fallback to GBP/UK if settings are invalid
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.UK);
            return formatter.format(safeAmount);
        }
    }

    /**
     * Format a percentage value.
     *
     * @param value the percentage value (null-safe, defaults to zero)
     * @return formatted percentage string (e.g., "25.5%")
     */
    public String formatPercentage(final BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        return String.format("%.1f%%", safeValue);
    }
}
