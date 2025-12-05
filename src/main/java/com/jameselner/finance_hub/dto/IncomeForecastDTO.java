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
public class IncomeForecastDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalForecastedIncome;
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageWeeklyIncome;
    private List<ForecastedIncomeEntry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastedIncomeEntry {
        private LocalDate expectedDate;
        private String sourceName;
        private BigDecimal amount;
        private String frequency;
    }
}
