package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeReportDTO {
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private BigDecimal totalIncome;
    private BigDecimal recurringIncome;
    private BigDecimal oneTimeIncome;
    private BigDecimal averageMonthlyIncome;
    private Long activeSourcesCount;
    private Long recurringSourcesCount;
    private BigDecimal highestIncomeSource;
    private String highestIncomeSourceName;
    private Map<String, BigDecimal> incomeByCategory;
    private Map<String, BigDecimal> incomeBySource;
}
