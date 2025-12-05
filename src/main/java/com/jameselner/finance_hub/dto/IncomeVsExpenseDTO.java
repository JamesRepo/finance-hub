package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeVsExpenseDTO {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;
    private BigDecimal savingsRate;
    private Boolean isPositive;
    private String status;
}
