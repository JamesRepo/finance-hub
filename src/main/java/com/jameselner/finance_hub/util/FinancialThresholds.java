package com.jameselner.finance_hub.util;

import java.math.BigDecimal;

/**
 * Constants for financial thresholds used throughout the application.
 * Centralizes magic numbers for better maintainability.
 */
public final class FinancialThresholds {

    private FinancialThresholds() {
        // Prevent instantiation
    }

    // Savings rate thresholds
    /** Savings rate at or above this percentage is considered good */
    public static final BigDecimal GOOD_SAVINGS_RATE_PERCENT = BigDecimal.valueOf(20);

    // Housing thresholds
    /** Housing costs above this percentage of income triggers a warning */
    public static final BigDecimal WARNING_HOUSING_RATIO_PERCENT = BigDecimal.valueOf(30);

    // Budget thresholds
    /** Budget utilization above this percentage means over budget */
    public static final BigDecimal BUDGET_EXCEEDED_PERCENT = BigDecimal.valueOf(100);

    // Category filtering
    /** Minimum amount for a category to be displayed in summaries */
    public static final BigDecimal MINIMUM_CATEGORY_AMOUNT = BigDecimal.ONE;

    // Synthetic category IDs for non-transaction expenses
    /** ID used for the synthetic Housing category in spending breakdowns */
    public static final Long HOUSING_CATEGORY_ID = -1L;

    /** ID used for the synthetic Debt Payments category in spending breakdowns */
    public static final Long DEBT_CATEGORY_ID = -2L;

    /** ID used for the synthetic Subscriptions category in spending breakdowns */
    public static final Long SUBSCRIPTIONS_CATEGORY_ID = -3L;

    /** ID used for the synthetic Holidays category in spending breakdowns */
    public static final Long HOLIDAYS_CATEGORY_ID = -4L;

    // Synthetic category colors
    public static final String HOUSING_CATEGORY_COLOR = "#4A90A4";
    public static final String DEBT_CATEGORY_COLOR = "#E57373";
    public static final String SUBSCRIPTIONS_CATEGORY_COLOR = "#9575CD";
    public static final String HOLIDAYS_CATEGORY_COLOR = "#4DB6AC";
}
