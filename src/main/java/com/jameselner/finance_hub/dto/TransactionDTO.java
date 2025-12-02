package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.TransactionType;
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
public class TransactionDTO {

    private Long transactionId;

    private Long accountId;
    private String accountName;

    private Long categoryId;
    private String categoryName;
    private String categoryColorCode;

    private TransactionType transactionType;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String placeVenue;
    private String description;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}