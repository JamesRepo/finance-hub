package com.jameselner.finance_hub.dto;

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
public class DebtPaymentDTO {

    private Long paymentId;

    private Long debtId;
    private String debtName;

    private BigDecimal paymentAmount;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private LocalDate paymentDate;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
