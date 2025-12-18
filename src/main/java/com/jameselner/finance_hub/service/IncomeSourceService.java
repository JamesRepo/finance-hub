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

    public List<IncomeSourceDTO> findActiveByUserAsDto(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.findByUserAndIsActiveOrderByStartDateDesc(user, true).stream()
                .map(incomeSourceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<IncomeSourceDTO> findRecurringByUserAsDto(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.findByUserAndIsRecurringOrderByStartDateDesc(user, true).stream()
                .map(incomeSourceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<IncomeSourceDTO> findActiveRecurringByUserAsDto(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.findByUserAndIsActiveAndIsRecurringOrderByNextExpectedDateAsc(
                user, true, true).stream()
                .map(incomeSourceMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<IncomeSourceDTO> findDueRecurringIncomeAsDto(final User user, final LocalDate upToDate) {
        validateUserNotNull(user);
        if (upToDate == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
        return incomeSourceRepository.findDueRecurringIncome(user, upToDate).stream()
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

    @Transactional
    public void deactivateById(final Long id) {
        validateIdNotNull(id);

        IncomeSource incomeSource = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + id + " does not exist"));

        incomeSource.setIsActive(false);
        incomeSourceRepository.save(incomeSource);

        log.debug("Deactivated income source with ID {}", id);
    }

    @Transactional
    public void activateById(final Long id) {
        validateIdNotNull(id);

        IncomeSource incomeSource = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + id + " does not exist"));

        incomeSource.setIsActive(true);
        incomeSourceRepository.save(incomeSource);

        log.debug("Activated income source with ID {}", id);
    }

    @Transactional
    public IncomeSourceDTO processRecurringIncome(final Long id) {
        validateIdNotNull(id);

        IncomeSource incomeSource = incomeSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Income source with ID " + id + " does not exist"));

        if (!incomeSource.getIsRecurring()) {
            throw new IllegalArgumentException("Income source is not recurring");
        }

        if (!incomeSource.getIsActive()) {
            throw new IllegalArgumentException("Income source is not active");
        }

        LocalDate nextDate = calculateNextOccurrence(
                incomeSource.getNextExpectedDate(),
                incomeSource.getRecurrenceFrequency());

        if (incomeSource.getEndDate() != null && nextDate.isAfter(incomeSource.getEndDate())) {
            incomeSource.setIsActive(false);
            log.debug("Income source {} has reached its end date, deactivating", id);
        } else {
            incomeSource.setNextExpectedDate(nextDate);
        }

        IncomeSource savedIncomeSource = incomeSourceRepository.save(incomeSource);
        return incomeSourceMapper.toDto(savedIncomeSource);
    }

    public boolean existsById(final Long id) {
        validateIdNotNull(id);
        return incomeSourceRepository.existsById(id);
    }

    public long count() {
        return incomeSourceRepository.count();
    }

    public BigDecimal getTotalActiveIncomeByUser(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.getTotalActiveIncomeByUser(user);
    }

    public BigDecimal getTotalRecurringIncomeByUser(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.getTotalRecurringIncomeByUser(user);
    }

    public long countActiveIncomeSourcesByUser(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.countActiveIncomeSourcesByUser(user);
    }

    public long countRecurringIncomeSourcesByUser(final User user) {
        validateUserNotNull(user);
        return incomeSourceRepository.countRecurringIncomeSourcesByUser(user);
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
