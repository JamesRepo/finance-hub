package com.jameselner.finance_hub.mapper;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IncomeSourceMapper {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IncomeDeductionMapper incomeDeductionMapper;

    public IncomeSourceDTO toDto(final IncomeSource incomeSource) {
        if (incomeSource == null) {
            return null;
        }

        return IncomeSourceDTO.builder()
                .incomeSourceId(incomeSource.getIncomeSourceId())
                .userId(incomeSource.getUser() != null ? incomeSource.getUser().getUserId() : null)
                .userName(incomeSource.getUser() != null ? incomeSource.getUser().getEmail() : null)
                .sourceName(incomeSource.getSourceName())
                .description(incomeSource.getDescription())
                .amount(incomeSource.getAmount())
                .grossAmount(incomeSource.getGrossAmount())
                .netAmount(incomeSource.getNetAmount())
                .deductions(incomeSource.getDeductions() != null ?
                        incomeSource.getDeductions().stream()
                                .map(incomeDeductionMapper::toDto)
                                .collect(Collectors.toList()) : null)
                .isRecurring(incomeSource.getIsRecurring())
                .recurrenceFrequency(incomeSource.getRecurrenceFrequency())
                .startDate(incomeSource.getStartDate())
                .endDate(incomeSource.getEndDate())
                .nextExpectedDate(incomeSource.getNextExpectedDate())
                .isActive(incomeSource.getIsActive())
                .categoryId(incomeSource.getCategory() != null ? incomeSource.getCategory().getCategoryId() : null)
                .categoryName(incomeSource.getCategory() != null ? incomeSource.getCategory().getCategoryName() : null)
                .autoCreateTransaction(incomeSource.getAutoCreateTransaction())
                .createdAt(incomeSource.getCreatedAt())
                .updatedAt(incomeSource.getUpdatedAt())
                .build();
    }

    public IncomeSource toEntity(final IncomeSourceDTO dto) {
        if (dto == null) {
            return null;
        }

        IncomeSource incomeSource = new IncomeSource();
        updateEntityFromDto(incomeSource, dto);
        return incomeSource;
    }

    public void updateEntityFromDto(final IncomeSource incomeSource, final IncomeSourceDTO dto) {
        if (incomeSource == null || dto == null) {
            return;
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User with ID " + dto.getUserId() + " not found"));
            incomeSource.setUser(user);
        }

        incomeSource.setSourceName(dto.getSourceName());
        incomeSource.setDescription(dto.getDescription());
        incomeSource.setAmount(dto.getAmount());
        incomeSource.setGrossAmount(dto.getGrossAmount());
        incomeSource.setNetAmount(dto.getNetAmount());
        incomeSource.setIsRecurring(dto.getIsRecurring());
        incomeSource.setRecurrenceFrequency(dto.getRecurrenceFrequency());
        incomeSource.setStartDate(dto.getStartDate());
        incomeSource.setEndDate(dto.getEndDate());
        incomeSource.setNextExpectedDate(dto.getNextExpectedDate());
        incomeSource.setIsActive(dto.getIsActive());
        incomeSource.setAutoCreateTransaction(dto.getAutoCreateTransaction());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElse(null);
            incomeSource.setCategory(category);
        } else {
            incomeSource.setCategory(null);
        }
    }
}
