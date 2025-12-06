package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.AccountType;
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
public class AccountDTO {
    private Long accountId;
    private Long userId;
    private String userEmail;
    private String accountName;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
