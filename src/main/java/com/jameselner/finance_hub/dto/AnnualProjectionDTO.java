package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualProjectionDTO {

    private Integer year;

    // Projected annual totals
    private BigDecimal projectedIncome;
    private BigDecimal projectedExpenses;
    private BigDecimal projectedNetSavings;
    private BigDecimal projectedSavingsRate;

    // Expense breakdown by source
    private BigDecimal budgetedExpenses;
    private BigDecimal housingCosts;
    private BigDecimal debtPayments;
    private BigDecimal subscriptionCosts;
    private BigDecimal holidayCosts;

    // Category-level projections
    private List<CategoryProjection> categoryProjections;

    // For current year: blended actual + projected
    private BigDecimal ytdActualIncome;
    private BigDecimal ytdActualExpenses;
    private BigDecimal remainingProjectedIncome;
    private BigDecimal remainingProjectedExpenses;
    private BigDecimal blendedAnnualIncome;
    private BigDecimal blendedAnnualExpenses;
    private BigDecimal blendedNetSavings;
    private BigDecimal blendedSavingsRate;
    private int completedMonths;
    private int projectedMonths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryProjection {
        private Long categoryId;
        private String categoryName;
        private String colorCode;
        private BigDecimal annualAmount;
        private BigDecimal percentageOfTotal;
        private String periodType;
        private boolean synthetic;
    }
}
