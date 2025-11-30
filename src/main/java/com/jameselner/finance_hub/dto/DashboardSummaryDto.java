package com.jameselner.finance_hub.dto;

import java.math.BigDecimal;

public record DashboardSummaryDto(
    BigDecimal totalBalance,
    BigDecimal monthlyIncome,
    BigDecimal monthlyExpenses,
    BigDecimal totalDebt,
    BigDecimal previousMonthIncome,
    BigDecimal previousMonthExpenses,
    BigDecimal previousMonthTotalDebt
) {
    public BigDecimal getIncomeChange() {
        if (previousMonthIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return monthlyIncome.subtract(previousMonthIncome)
                .divide(previousMonthIncome, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getExpenseChange() {
        if (previousMonthExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return monthlyExpenses.subtract(previousMonthExpenses)
                .divide(previousMonthExpenses, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
