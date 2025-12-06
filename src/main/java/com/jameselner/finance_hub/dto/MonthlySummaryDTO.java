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
    private BigDecimal netSavings;
    private BigDecimal savingsRate; // Percentage

    // Budget Performance
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal budgetRemaining;
    private BigDecimal budgetUtilization; // Percentage
    private int budgetsOverCount;
    private int budgetsOnTrackCount;

    // Top Categories
    private List<CategorySpendingDTO> topSpendingCategories;

    // Housing
    private BigDecimal housingCosts;
    private BigDecimal housingToIncomeRatio;

    // Debt
    private BigDecimal debtPayments;
    private BigDecimal totalDebt;

    // Statistics
    private int transactionCount;
    private BigDecimal averageTransactionSize;
    private BigDecimal largestExpense;
    private String largestExpenseCategory;

    // Month-over-Month Comparison
    private MonthComparisonDTO monthComparison;
}
