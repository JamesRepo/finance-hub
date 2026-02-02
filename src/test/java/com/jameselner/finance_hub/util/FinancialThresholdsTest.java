package com.jameselner.finance_hub.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FinancialThresholds Tests")
class FinancialThresholdsTest {

    @Test
    @DisplayName("Good savings rate should be 20%")
    void goodSavingsRateShouldBe20Percent() {
        assertEquals(BigDecimal.valueOf(20), FinancialThresholds.GOOD_SAVINGS_RATE_PERCENT);
    }

    @Test
    @DisplayName("Warning housing ratio should be 30%")
    void warningHousingRatioShouldBe30Percent() {
        assertEquals(BigDecimal.valueOf(30), FinancialThresholds.WARNING_HOUSING_RATIO_PERCENT);
    }

    @Test
    @DisplayName("Budget exceeded threshold should be 100%")
    void budgetExceededShouldBe100Percent() {
        assertEquals(BigDecimal.valueOf(100), FinancialThresholds.BUDGET_EXCEEDED_PERCENT);
    }

    @Test
    @DisplayName("Minimum category amount should be 1")
    void minimumCategoryAmountShouldBeOne() {
        assertEquals(BigDecimal.ONE, FinancialThresholds.MINIMUM_CATEGORY_AMOUNT);
    }

    @Test
    @DisplayName("Synthetic category IDs should be negative")
    void syntheticCategoryIdsShouldBeNegative() {
        assertTrue(FinancialThresholds.HOUSING_CATEGORY_ID < 0);
        assertTrue(FinancialThresholds.DEBT_CATEGORY_ID < 0);
        assertTrue(FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID < 0);
    }

    @Test
    @DisplayName("Synthetic category IDs should be unique")
    void syntheticCategoryIdsShouldBeUnique() {
        assertNotEquals(FinancialThresholds.HOUSING_CATEGORY_ID, FinancialThresholds.DEBT_CATEGORY_ID);
        assertNotEquals(FinancialThresholds.HOUSING_CATEGORY_ID, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID);
        assertNotEquals(FinancialThresholds.DEBT_CATEGORY_ID, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID);
    }

    @Test
    @DisplayName("Housing category ID should be -1")
    void housingCategoryIdShouldBeMinusOne() {
        assertEquals(-1L, FinancialThresholds.HOUSING_CATEGORY_ID);
    }

    @Test
    @DisplayName("Debt category ID should be -2")
    void debtCategoryIdShouldBeMinusTwo() {
        assertEquals(-2L, FinancialThresholds.DEBT_CATEGORY_ID);
    }

    @Test
    @DisplayName("Subscriptions category ID should be -3")
    void subscriptionsCategoryIdShouldBeMinusThree() {
        assertEquals(-3L, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID);
    }

    @Test
    @DisplayName("Category colors should be valid hex colors")
    void categoryColorsShouldBeValidHexColors() {
        assertTrue(isValidHexColor(FinancialThresholds.HOUSING_CATEGORY_COLOR));
        assertTrue(isValidHexColor(FinancialThresholds.DEBT_CATEGORY_COLOR));
        assertTrue(isValidHexColor(FinancialThresholds.SUBSCRIPTIONS_CATEGORY_COLOR));
    }

    @Test
    @DisplayName("Category colors should be unique")
    void categoryColorsShouldBeUnique() {
        assertNotEquals(FinancialThresholds.HOUSING_CATEGORY_COLOR, FinancialThresholds.DEBT_CATEGORY_COLOR);
        assertNotEquals(FinancialThresholds.HOUSING_CATEGORY_COLOR, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_COLOR);
        assertNotEquals(FinancialThresholds.DEBT_CATEGORY_COLOR, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_COLOR);
    }

    private boolean isValidHexColor(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }
}
