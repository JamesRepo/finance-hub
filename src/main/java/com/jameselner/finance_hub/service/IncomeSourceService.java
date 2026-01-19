package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.mapper.IncomeSourceMapper;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeSourceService {

    private final IncomeSourceRepository incomeSourceRepository;
    private final IncomeSourceMapper incomeSourceMapper;

    @Transactional
    public IncomeSourceDTO createIncomeSourceFromDto(final IncomeSourceDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getIncomeSourceId() != null) {
            throw new IllegalArgumentException("Income source ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validateRecurrenceLogic(dto);
        validateDates(dto);

        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }

        if (dto.getAutoCreateTransaction() == null) {
            dto.setAutoCreateTransaction(false);
        }

        calculateNextExpectedDate(dto);

        IncomeSource incomeSource = incomeSourceMapper.toEntity(dto);
        IncomeSource savedIncomeSource = incomeSourceRepository.save(incomeSource);
        return incomeSourceMapper.toDto(savedIncomeSource);
    }

    @Transactional
    public IncomeSourceDTO updateIncomeSourceFromDto(final IncomeSourceDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getIncomeSourceId() == null) {
            throw new IllegalArgumentException("Income source ID must not be null for update operation");
        }

        IncomeSource existingIncomeSource = incomeSourceRepository.findById(dto.getIncomeSourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + dto.getIncomeSourceId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validateRecurrenceLogic(dto);
        validateDates(dto);

        calculateNextExpectedDate(dto);

        incomeSourceMapper.updateEntityFromDto(existingIncomeSource, dto);
        IncomeSource savedIncomeSource = incomeSourceRepository.save(existingIncomeSource);
        return incomeSourceMapper.toDto(savedIncomeSource);
    }

    public Optional<IncomeSourceDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return incomeSourceRepository.findById(id)
                .map(incomeSourceMapper::toDto);
    }

    public List<IncomeSourceDTO> findAllAsDto() {
        return incomeSourceRepository.findAll().stream()
                .map(incomeSourceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<IncomeSourceDTO> findByUserAsDto(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.findByUserOrderByStartDateDesc(user).stream()
                .map(incomeSourceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting income source with ID {}", id);

        if (!incomeSourceRepository.existsById(id)) {
            throw new IllegalArgumentException("Income source with ID " + id + " does not exist");
        }

        incomeSourceRepository.deleteById(id);
    }

    /**
     * Calculate total income from income sources for a specific period.
     * Sums all income sources where the start date falls within the period.
     * Uses net amount if available, otherwise gross amount.
     */
    public BigDecimal calculateTotalForPeriod(final User user, final LocalDate startDate, final LocalDate endDate) {
        validateUserNotNull(user);
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }

        List<IncomeSource> incomeSources = incomeSourceRepository.findActiveInPeriod(user, startDate, endDate);

        log.debug("Calculating income for period {} to {}: found {} income sources",
                startDate, endDate, incomeSources.size());

        BigDecimal total = BigDecimal.ZERO;
        for (IncomeSource source : incomeSources) {
            BigDecimal amount = source.getNetAmount() != null ? source.getNetAmount() : source.getGrossAmount();
            log.debug("  Source '{}' (id={}, date={}): {}",
                    source.getDescription(),
                    source.getIncomeSourceId(),
                    source.getStartDate(),
                    amount);
            total = total.add(amount);
        }

        log.debug("Total income from sources for {} to {}: {}", startDate, endDate, total);
        return total;
    }

    private void calculateNextExpectedDate(final IncomeSourceDTO dto) {
        if (dto.getIsRecurring() != null && dto.getIsRecurring() && dto.getRecurrenceFrequency() != null) {
            if (dto.getNextExpectedDate() == null) {
                dto.setNextExpectedDate(calculateNextOccurrence(dto.getStartDate(), dto.getRecurrenceFrequency()));
            }
        } else {
            dto.setNextExpectedDate(null);
        }
    }

    private LocalDate calculateNextOccurrence(final LocalDate currentDate, final RecurrenceFrequency frequency) {
        if (currentDate == null || frequency == null) {
            return null;
        }

        return switch (frequency) {
            case ONE_TIME -> null;
            case WEEKLY -> currentDate.plusWeeks(1);
            case BI_WEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentDate.plusMonths(6);
            case ANNUALLY -> currentDate.plusYears(1);
        };
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Income source ID must not be null");
        }
    }

    private void validateUserNotNull(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
    }

    private void validateDtoNotNull(final IncomeSourceDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("IncomeSourceDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final IncomeSourceDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getGrossAmount() == null) {
            throw new IllegalArgumentException("Gross amount must not be null");
        }
        if (dto.getIsRecurring() == null) {
            throw new IllegalArgumentException("Is recurring must not be null");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
    }

    private void validateAmount(final IncomeSourceDTO dto) {
        if (dto.getGrossAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateRecurrenceLogic(final IncomeSourceDTO dto) {
        if (dto.getIsRecurring() != null && dto.getIsRecurring()) {
            if (dto.getRecurrenceFrequency() == null) {
                throw new IllegalArgumentException("Recurrence frequency must be specified for recurring income");
            }
            if (dto.getRecurrenceFrequency() == RecurrenceFrequency.ONE_TIME) {
                throw new IllegalArgumentException("Cannot use ONE_TIME frequency for recurring income");
            }
        }
    }

    private void validateDates(final IncomeSourceDTO dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new IllegalArgumentException("End date must be after or equal to start date");
            }
        }
    }
}
