package com.jameselner.finance_hub.dto;

import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeSourceDTO {
    private Long incomeSourceId;
    private Long userId;
    private String userName;
    private String description;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    @Builder.Default
    private List<IncomeDeductionDTO> deductions = new ArrayList<>();
    private Boolean isRecurring;
    private RecurrenceFrequency recurrenceFrequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExpectedDate;
    private Boolean isActive;
    private Long categoryId;
    private String categoryName;
    private Boolean autoCreateTransaction;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
