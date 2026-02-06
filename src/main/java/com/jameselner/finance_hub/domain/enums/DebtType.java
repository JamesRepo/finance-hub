package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

@Getter
public enum DebtType {
    CREDIT_CARD("Credit Card"),
    STUDENT_LOAN("Student Loan"),
    MORTGAGE("Mortgage"),
    AUTO_LOAN("Auto Loan"),
    PERSONAL_LOAN("Personal Loan"),
    OTHER("Other");

    private final String displayName;

    DebtType(final String displayName) {
        this.displayName = displayName;
    }
}
