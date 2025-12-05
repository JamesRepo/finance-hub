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
public class SavingsGoalProgressDTO {

    private Long goalId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal percentageComplete;
    private LocalDate targetDate;
    private Long daysRemaining;
    private BigDecimal recommendedMonthlyContribution;
    private BigDecimal recommendedWeeklyContribution;
    private boolean onTrack;
    private String status;
}
