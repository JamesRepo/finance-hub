package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.DebtType;
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
public class DebtDTO {

    private Long debtId;

    private Long userId;
    private String userName;

    private String debtName;
    private DebtType debtType;
    private BigDecimal principalAmount;
    private BigDecimal currentBalance;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate targetPayoffDate;
    private BigDecimal minimumPayment;
    private String notes;
    private Boolean active;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
