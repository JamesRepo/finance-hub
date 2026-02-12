package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDTO {
    private YearMonth month;

    // Income & Expense Totals
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal transactionExpenses; // Expenses from transactions only (excluding housing)

    // Budget Performance
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal budgetRemaining;
    private BigDecimal budgetUtilization; // Percentage
    private int budgetsOverCount;
    private int budgetsOnTrackCount;

    // Top Categories
    private List<CategorySpendingDTO> topSpendingCategories;
    private List<CategorySpendingDTO> variableSpendingCategories;
    private List<CategorySpendingDTO> fixedCostCategories;

    // Housing & Holidays
    private BigDecimal housingCosts;
    private BigDecimal holidayCosts;
    private BigDecimal housingToIncomeRatio;

    // Statistics
    private int transactionCount;
    private BigDecimal averageTransactionSize;
    private BigDecimal largestExpense;
    private String largestExpenseCategory;

    // Month-over-Month Comparison
    private MonthComparisonDTO monthComparison;
}
