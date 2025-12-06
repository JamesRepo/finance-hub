package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
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
public class SubscriptionDTO {
    private Long subscriptionId;
    private Long userId;
    private String userEmail;
    private String name;
    private BigDecimal amount;
    private SubscriptionFrequency frequency;
    private LocalDate paymentDate;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Calculated fields for analytics
    private BigDecimal monthlyEquivalent;
    private BigDecimal annualEquivalent;
}
