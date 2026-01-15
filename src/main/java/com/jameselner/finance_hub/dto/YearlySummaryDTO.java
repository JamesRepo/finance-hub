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
public class YearlySummaryDTO {
    private Integer year;

    // Annual Totals
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal transactionExpenses; // Expenses from transactions only (excluding housing)
    private BigDecimal netSavings;
    private BigDecimal savingsRate; // Percentage

    // Monthly Breakdown
    private List<MonthlyDataPointDTO> monthlyData;

    // Statistics
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageMonthlyExpenses;
    private BigDecimal averageMonthlySavings;

    private String highestIncomeMonth;
    private BigDecimal highestIncomeAmount;

    private String highestExpenseMonth;
    private BigDecimal highestExpenseAmount;

    private String bestSavingsMonth;
    private BigDecimal bestSavingsAmount;

    // Category Analysis
    private List<CategorySpendingDTO> topSpendingCategories;
    private BigDecimal totalCategorySpending;

    // Year-over-Year Comparison
    private YearComparisonDTO yearComparison;

    // Budget Performance
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal budgetUtilization; // Percentage

    // Housing & Debt
    private BigDecimal totalHousingCosts;
    private BigDecimal totalDebtPayments;

    // Transaction Statistics
    private int totalTransactions;
    private BigDecimal largestTransaction;
    private String largestTransactionCategory;
}
