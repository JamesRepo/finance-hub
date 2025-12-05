package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeDeduction;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.dto.IncomeDeductionDTO;
import com.jameselner.finance_hub.mapper.IncomeDeductionMapper;
import com.jameselner.finance_hub.repository.IncomeDeductionRepository;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeDeductionService {

    private final IncomeDeductionRepository incomeDeductionRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final IncomeDeductionMapper incomeDeductionMapper;

    @Transactional
    public IncomeDeductionDTO createDeductionFromDto(final IncomeDeductionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getDeductionId() != null) {
            throw new IllegalArgumentException("Deduction ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validatePercentageLogic(dto);

        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }

        IncomeDeduction deduction = incomeDeductionMapper.toEntity(dto);
        IncomeDeduction savedDeduction = incomeDeductionRepository.save(deduction);
        return incomeDeductionMapper.toDto(savedDeduction);
    }

    @Transactional
    public IncomeDeductionDTO updateDeductionFromDto(final IncomeDeductionDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getDeductionId() == null) {
            throw new IllegalArgumentException("Deduction ID must not be null for update operation");
        }

        IncomeDeduction existingDeduction = incomeDeductionRepository.findById(dto.getDeductionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Deduction with ID " + dto.getDeductionId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validatePercentageLogic(dto);

        incomeDeductionMapper.updateEntityFromDto(existingDeduction, dto);
        IncomeDeduction savedDeduction = incomeDeductionRepository.save(existingDeduction);
        return incomeDeductionMapper.toDto(savedDeduction);
    }

    public Optional<IncomeDeductionDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return incomeDeductionRepository.findById(id)
                .map(incomeDeductionMapper::toDto);
    }

    public List<IncomeDeductionDTO> findByIncomeSourceAsDto(final Long incomeSourceId) {
        if (incomeSourceId == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }

        IncomeSource incomeSource = incomeSourceRepository.findById(incomeSourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + incomeSourceId + " does not exist"));

        return incomeDeductionRepository.findByIncomeSourceOrderByDeductionTypeAsc(incomeSource).stream()
                .map(incomeDeductionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<IncomeDeductionDTO> findActiveByIncomeSourceAsDto(final Long incomeSourceId) {
        if (incomeSourceId == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }

        IncomeSource incomeSource = incomeSourceRepository.findById(incomeSourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + incomeSourceId + " does not exist"));

        return incomeDeductionRepository.findByIncomeSourceAndIsActiveOrderByDeductionTypeAsc(
                incomeSource, true).stream()
                .map(incomeDeductionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting deduction with ID {}", id);

        if (!incomeDeductionRepository.existsById(id)) {
            throw new IllegalArgumentException("Deduction with ID " + id + " does not exist");
        }

        incomeDeductionRepository.deleteById(id);
    }

    public BigDecimal getTotalDeductionsByIncomeSource(final Long incomeSourceId) {
        if (incomeSourceId == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }

        IncomeSource incomeSource = incomeSourceRepository.findById(incomeSourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + incomeSourceId + " does not exist"));

        return incomeDeductionRepository.getTotalDeductionsByIncomeSource(incomeSource);
    }

    public long countActiveDeductionsByIncomeSource(final Long incomeSourceId) {
        if (incomeSourceId == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }

        IncomeSource incomeSource = incomeSourceRepository.findById(incomeSourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + incomeSourceId + " does not exist"));

        return incomeDeductionRepository.countActiveDeductionsByIncomeSource(incomeSource);
    }

    public BigDecimal calculateDeductionAmount(
            final BigDecimal grossAmount,
            final Boolean isPercentage,
            final BigDecimal amount,
            final BigDecimal percentageValue) {

        if (grossAmount == null) {
            throw new IllegalArgumentException("Gross amount must not be null");
        }

        if (isPercentage != null && isPercentage) {
            if (percentageValue == null) {
                throw new IllegalArgumentException("Percentage value must not be null when using percentage");
            }
            return grossAmount.multiply(percentageValue).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            if (amount == null) {
                throw new IllegalArgumentException("Amount must not be null when not using percentage");
            }
            return amount;
        }
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Deduction ID must not be null");
        }
    }

    private void validateDtoNotNull(final IncomeDeductionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("IncomeDeductionDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final IncomeDeductionDTO dto) {
        if (dto.getIncomeSourceId() == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }
        if (dto.getDeductionType() == null) {
            throw new IllegalArgumentException("Deduction type must not be null");
        }
        if (dto.getDeductionName() == null || dto.getDeductionName().trim().isEmpty()) {
            throw new IllegalArgumentException("Deduction name must not be null or empty");
        }
        if (dto.getIsPercentage() == null) {
            throw new IllegalArgumentException("Is percentage must not be null");
        }
    }

    private void validateAmount(final IncomeDeductionDTO dto) {
        if (!dto.getIsPercentage() && dto.getAmount() != null) {
            if (dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount must not be negative");
            }
        }
    }

    private void validatePercentageLogic(final IncomeDeductionDTO dto) {
        if (dto.getIsPercentage() != null && dto.getIsPercentage()) {
            if (dto.getPercentageValue() == null) {
                throw new IllegalArgumentException("Percentage value must be specified when using percentage");
            }
            if (dto.getPercentageValue().compareTo(BigDecimal.ZERO) < 0 ||
                    dto.getPercentageValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage value must be between 0 and 100");
            }
        } else {
            if (dto.getAmount() == null) {
                throw new IllegalArgumentException("Amount must be specified when not using percentage");
            }
        }
    }
}
