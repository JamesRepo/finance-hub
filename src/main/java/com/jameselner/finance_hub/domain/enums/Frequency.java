package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public enum Frequency {
    WEEKLY("Weekly", 52),
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    YEARLY("Yearly", 1),
    ONE_TIME("One-time", 0);

    private final String displayName;
    private final int periodsPerYear;

    Frequency(final String displayName, final int periodsPerYear) {
        this.displayName = displayName;
        this.periodsPerYear = periodsPerYear;
    }

    /**
     * Convert an amount with this frequency to a monthly equivalent
     * @param amount The amount at this frequency
     * @return The monthly equivalent amount
     */
    public BigDecimal toMonthlyEquivalent(final BigDecimal amount) {
        if (amount == null || this == ONE_TIME) {
            return BigDecimal.ZERO;
        }

        return amount.multiply(BigDecimal.valueOf(periodsPerYear))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    /**
     * Convert an amount with this frequency to an annual equivalent
     * @param amount The amount at this frequency
     * @return The annual equivalent amount
     */
    public BigDecimal toAnnualEquivalent(final BigDecimal amount) {
        if (amount == null || this == ONE_TIME) {
            return BigDecimal.ZERO;
        }

        return amount.multiply(BigDecimal.valueOf(periodsPerYear));
    }

    public boolean isRecurring() {
        return this != ONE_TIME;
    }
}
