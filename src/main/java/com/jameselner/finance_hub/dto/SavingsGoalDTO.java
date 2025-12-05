package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.Priority;
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
public class SavingsGoalDTO {

    private Long goalId;

    private Long userId;
    private String userName;

    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private Priority priority;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
