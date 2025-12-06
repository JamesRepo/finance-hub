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
public class CategorySpendingDTO {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal totalSpent;
    private BigDecimal budgetAmount;
    private BigDecimal percentageOfTotal;
    private int transactionCount;
}
