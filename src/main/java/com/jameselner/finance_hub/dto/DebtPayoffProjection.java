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
public class DebtPayoffProjection {

    private Long debtId;
    private String debtName;
    private BigDecimal currentBalance;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private int monthsToPayoff;
    private LocalDate projectedPayoffDate;
    private BigDecimal totalInterestPaid;
    private BigDecimal totalPaid;
}
