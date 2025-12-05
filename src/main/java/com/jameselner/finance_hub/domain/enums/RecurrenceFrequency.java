package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

@Getter
public enum RecurrenceFrequency {
    ONE_TIME("One-time", 0),
    WEEKLY("Weekly", 7),
    BI_WEEKLY("Bi-weekly", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    SEMI_ANNUALLY("Semi-annually", 180),
    ANNUALLY("Annually", 365);

    private final String displayName;
    private final int daysInterval;

    RecurrenceFrequency(final String displayName, final int daysInterval) {
        this.displayName = displayName;
        this.daysInterval = daysInterval;
    }

    public boolean isRecurring() {
        return this != ONE_TIME;
    }
}
