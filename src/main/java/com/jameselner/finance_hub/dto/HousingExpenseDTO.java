package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.Frequency;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
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
public class HousingExpenseDTO {
    private Long expenseId;
    private Long userId;
    private String userEmail;
    private HousingExpenseType expenseType;
    private BigDecimal amount;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Calculated fields for analytics
    private BigDecimal monthlyEquivalent;
    private BigDecimal annualEquivalent;
}
