package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthComparisonDTO {
    private BigDecimal incomeChange;
    private BigDecimal incomeChangePercent;

    private BigDecimal expenseChange;
    private BigDecimal expenseChangePercent;
}
