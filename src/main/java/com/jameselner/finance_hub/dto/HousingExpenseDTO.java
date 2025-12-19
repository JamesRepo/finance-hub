package com.jameselner.finance_hub.dto;

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
    private LocalDate expenseMonth;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
