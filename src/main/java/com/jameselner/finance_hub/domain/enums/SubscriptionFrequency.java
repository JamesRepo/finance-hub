package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public enum SubscriptionFrequency {
    MONTHLY("Monthly", 1),
    YEARLY("Yearly", 12);

    private final String displayName;
    private final int monthsPerPeriod;

    SubscriptionFrequency(final String displayName, final int monthsPerPeriod) {
        this.displayName = displayName;
        this.monthsPerPeriod = monthsPerPeriod;
    }

    /**
     * Convert subscription amount to monthly equivalent
     * @param amount The subscription amount
     * @return Monthly equivalent amount
     */
    public BigDecimal toMonthlyEquivalent(final BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        return switch (this) {
            case MONTHLY -> amount;
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Convert subscription amount to annual equivalent
     * @param amount The subscription amount
     * @return Annual equivalent amount
     */
    public BigDecimal toAnnualEquivalent(final BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        return switch (this) {
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case YEARLY -> amount;
        };
    }
}
