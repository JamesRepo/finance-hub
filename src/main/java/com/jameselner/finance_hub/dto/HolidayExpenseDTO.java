package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.HolidayExpenseType;
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
public class HolidayExpenseDTO {
    private Long expenseId;
    private Long holidayId;
    private String holidayName;
    private HolidayExpenseType expenseType;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}