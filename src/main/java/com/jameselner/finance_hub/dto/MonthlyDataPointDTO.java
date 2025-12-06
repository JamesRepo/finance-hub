package com.jameselner.finance_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDataPointDTO {
    private YearMonth month;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal savings;
    private BigDecimal savingsRate;
}
