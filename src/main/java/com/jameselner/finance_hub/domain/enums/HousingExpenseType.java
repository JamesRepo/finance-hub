package com.jameselner.finance_hub.domain.enums;

import lombok.Getter;

@Getter
public enum HousingExpenseType {
    RENT("Rent"),
    MORTGAGE("Mortgage"),
    COUNCIL_TAX("Council Tax"),
    ENERGY("Energy"),
    INTERNET("Internet"),
    WATER("Water"),
    INSURANCE("Insurance"),
    UTILITIES("Utilities"),
    MAINTENANCE("Maintenance & Repairs"),
    HOA_FEES("HOA Fees"),
    CLEANER("Cleaner"),
    FURNITURE("Furniture"),
    OTHER("Other Housing Expense");

    private final String displayName;

    HousingExpenseType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Check if this expense type is a utility
     * @return true if this is a utility expense
     */
    public boolean isUtility() {
        return this == UTILITIES;
    }

    /**
     * Check if this is a primary housing cost (rent or mortgage)
     * @return true if this is rent or mortgage
     */
    public boolean isPrimaryHousing() {
        return this == RENT || this == MORTGAGE;
    }
}
