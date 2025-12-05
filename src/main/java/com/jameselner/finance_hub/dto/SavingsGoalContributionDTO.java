package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalContributionDTO {

    private Long contributionId;

    private Long goalId;
    private String goalName;

    private BigDecimal amount;
    private LocalDate contributionDate;
    private String note;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
