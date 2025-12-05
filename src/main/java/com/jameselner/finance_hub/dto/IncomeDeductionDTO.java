package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.DeductionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeDeductionDTO {
    private Long deductionId;
    private Long incomeSourceId;
    private DeductionType deductionType;
    private String deductionName;
    private BigDecimal amount;
    private Boolean isPercentage;
    private BigDecimal percentageValue;
    private String description;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
