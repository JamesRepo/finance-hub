package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtPayoffPlan {

    private String strategyName;
    private BigDecimal totalMonthlyPayment;
    private int totalMonthsToPayoff;
    private LocalDate finalPayoffDate;
    private BigDecimal totalInterestPaid;
    private BigDecimal totalAmountPaid;
    private List<DebtPayoffProjection> debtProjections;
}
