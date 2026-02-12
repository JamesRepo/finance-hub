package com.jameselner.finance_hub.dto;

import java.math.BigDecimal;

public record DashboardSummaryDto(
    BigDecimal totalBalance,
    BigDecimal lastMonthlyIncome,
    BigDecimal monthlyExpenses,
    BigDecimal totalDebt,
    BigDecimal totalSavings
) {
}
