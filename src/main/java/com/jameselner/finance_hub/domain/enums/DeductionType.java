package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

@Getter
public enum DeductionType {
    INCOME_TAX("Income Tax"),
    NATIONAL_INSURANCE("National Insurance"),
    PENSION("Pension Contribution"),
    STUDENT_LOAN("Student Loan"),
    POSTGRAD_LOAN("Postgraduate Loan"),
    DENTAL_INSURANCE("Dental Insurance"),
    HEALTH_INSURANCE("Health Insurance"),
    LIFE_INSURANCE("Life Insurance"),
    GYM_MEMBERSHIP("Gym Membership"),
    UNION_DUES("Union Dues"),
    PARKING("Parking"),
    OTHER("Other Deduction");

    private final String displayName;

    DeductionType(final String displayName) {
        this.displayName = displayName;
    }
}
