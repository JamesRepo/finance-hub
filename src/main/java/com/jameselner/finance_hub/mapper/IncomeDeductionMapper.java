package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.IncomeDeduction;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.dto.IncomeDeductionDTO;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncomeDeductionMapper {

    private final IncomeSourceRepository incomeSourceRepository;

    public IncomeDeductionDTO toDto(final IncomeDeduction deduction) {
        if (deduction == null) {
            return null;
        }

        return IncomeDeductionDTO.builder()
                .deductionId(deduction.getDeductionId())
                .incomeSourceId(deduction.getIncomeSource() != null ?
                        deduction.getIncomeSource().getIncomeSourceId() : null)
                .deductionType(deduction.getDeductionType())
                .deductionName(deduction.getDeductionName())
                .amount(deduction.getAmount())
                .isPercentage(deduction.getIsPercentage())
                .percentageValue(deduction.getPercentageValue())
                .description(deduction.getDescription())
                .isActive(deduction.getIsActive())
                .createdAt(deduction.getCreatedAt())
                .updatedAt(deduction.getUpdatedAt())
                .build();
    }

    public IncomeDeduction toEntity(final IncomeDeductionDTO dto) {
        if (dto == null) {
            return null;
        }

        IncomeDeduction deduction = new IncomeDeduction();
        updateEntityFromDto(deduction, dto);
        return deduction;
    }

    public void updateEntityFromDto(final IncomeDeduction deduction, final IncomeDeductionDTO dto) {
        if (deduction == null || dto == null) {
            return;
        }

        if (dto.getIncomeSourceId() != null) {
            IncomeSource incomeSource = incomeSourceRepository.findById(dto.getIncomeSourceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Income source with ID " + dto.getIncomeSourceId() + " not found"));
            deduction.setIncomeSource(incomeSource);
        }

        deduction.setDeductionType(dto.getDeductionType());
        deduction.setDeductionName(dto.getDeductionName());
        deduction.setAmount(dto.getAmount());
        deduction.setIsPercentage(dto.getIsPercentage());
        deduction.setPercentageValue(dto.getPercentageValue());
        deduction.setDescription(dto.getDescription());
        deduction.setIsActive(dto.getIsActive());
    }
}
