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
public class HolidayDTO {
    private Long holidayId;
    private Long userId;
    private String userEmail;
    private String name;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private String description;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Calculated fields for analytics
    private BigDecimal totalSpent;
    private BigDecimal budgetRemaining;
    private Integer numberOfDays;
    private Integer expenseCount;
    private BigDecimal dailyAverage;
}