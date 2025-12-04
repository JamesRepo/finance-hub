package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.PeriodType;
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
public class BudgetDTO {

    private Long budgetId;

    private Long userId;
    private String userEmail;

    private Long categoryId;
    private String categoryName;
    private String categoryColorCode;

    private BigDecimal amount;
    private PeriodType periodType;
    private LocalDate startDate;
    private LocalDate endDate;

    // Calculated fields
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal overage;
    private Double percentageUsed;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
