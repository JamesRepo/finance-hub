package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.HousingExpense;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.mapper.HousingExpenseMapper;
import com.jameselner.finance_hub.repository.HousingExpenseRepository;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingExpenseService {

    private final HousingExpenseRepository housingExpenseRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final UserRepository userRepository;
    private final HousingExpenseMapper housingExpenseMapper;

    /**
     * Create a new housing expense from DTO
     */
    @Transactional
    public HousingExpenseDTO createFromDto(final HousingExpenseDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getExpenseId() != null) {
            throw new IllegalArgumentException("Expense ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validateDates(dto);

        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }

        HousingExpense expense = housingExpenseMapper.toEntity(dto);
        HousingExpense savedExpense = housingExpenseRepository.save(expense);

        return enrichWithCalculatedFields(housingExpenseMapper.toDto(savedExpense));
    }

    /**
     * Update an existing housing expense from DTO
     */
    @Transactional
    public HousingExpenseDTO updateFromDto(final HousingExpenseDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getExpenseId() == null) {
            throw new IllegalArgumentException("Expense ID must not be null for update operation");
        }

        HousingExpense existingExpense = housingExpenseRepository.findById(dto.getExpenseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Housing expense with ID " + dto.getExpenseId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto);
        validateDates(dto);

        housingExpenseMapper.updateEntityFromDto(existingExpense, dto);
        HousingExpense savedExpense = housingExpenseRepository.save(existingExpense);

        return enrichWithCalculatedFields(housingExpenseMapper.toDto(savedExpense));
    }

    /**
     * Find housing expense by ID as DTO
     */
    public Optional<HousingExpenseDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return housingExpenseRepository.findById(id)
                .map(housingExpenseMapper::toDto)
                .map(this::enrichWithCalculatedFields);
    }

    /**
     * Find all housing expenses for a user
     */
    public List<HousingExpenseDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserOrderByStartDateDesc(user).stream()
                .map(housingExpenseMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find active housing expenses for a user
     */
    public List<HousingExpenseDTO> findActiveByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserAndIsActiveTrueOrderByStartDateDesc(user).stream()
                .map(housingExpenseMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find housing expenses by user and type
     */
    public List<HousingExpenseDTO> findByUserIdAndTypeAsDto(final Long userId, final HousingExpenseType type) {
        validateIdNotNull(userId);
        Objects.requireNonNull(type, "Expense type must not be null");
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserAndExpenseTypeOrderByStartDateDesc(user, type).stream()
                .map(housingExpenseMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Find housing expenses within a date range
     */
    public List<HousingExpenseDTO> findByUserIdAndDateRangeAsDto(
            final Long userId,
            final LocalDate startDate,
            final LocalDate endDate) {
        validateIdNotNull(userId);
        Objects.requireNonNull(startDate, "Start date must not be null");
        Objects.requireNonNull(endDate, "End date must not be null");
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserAndDateRange(user, startDate, endDate).stream()
                .map(housingExpenseMapper::toDto)
                .map(this::enrichWithCalculatedFields)
                .collect(Collectors.toList());
    }

    /**
     * Delete housing expense by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        log.debug("Deleting housing expense with ID {}", id);

        if (!housingExpenseRepository.existsById(id)) {
            throw new IllegalArgumentException("Housing expense with ID " + id + " does not exist");
        }

        housingExpenseRepository.deleteById(id);
    }

    /**
     * Calculate total monthly housing costs for a user
     */
    public BigDecimal calculateTotalMonthlyHousingCosts(final Long userId) {
        List<HousingExpenseDTO> activeExpenses = findActiveByUserIdAsDto(userId);

        return activeExpenses.stream()
                .map(HousingExpenseDTO::getMonthlyEquivalent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total annual housing costs for a user
     */
    public BigDecimal calculateTotalAnnualHousingCosts(final Long userId) {
        List<HousingExpenseDTO> activeExpenses = findActiveByUserIdAsDto(userId);

        return activeExpenses.stream()
                .map(HousingExpenseDTO::getAnnualEquivalent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate monthly housing costs by type
     */
    public Map<HousingExpenseType, BigDecimal> calculateMonthlyHousingCostsByType(final Long userId) {
        List<HousingExpenseDTO> activeExpenses = findActiveByUserIdAsDto(userId);

        return activeExpenses.stream()
                .collect(Collectors.groupingBy(
                        HousingExpenseDTO::getExpenseType,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                HousingExpenseDTO::getMonthlyEquivalent,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * Calculate total monthly utilities costs separately
     */
    public BigDecimal calculateTotalMonthlyUtilities(final Long userId) {
        List<HousingExpenseDTO> utilities = findByUserIdAndTypeAsDto(userId, HousingExpenseType.UTILITIES).stream()
                .filter(HousingExpenseDTO::getIsActive)
                .toList();

        return utilities.stream()
                .map(HousingExpenseDTO::getMonthlyEquivalent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate housing-to-income ratio as a percentage
     * Returns the percentage of monthly income that goes to housing costs
     */
    public BigDecimal calculateHousingToIncomeRatio(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        // Calculate total monthly housing costs
        BigDecimal monthlyHousingCosts = calculateTotalMonthlyHousingCosts(userId);

        // Calculate total monthly income from active income sources
        BigDecimal monthlyIncome = calculateMonthlyIncome(user);

        // Avoid division by zero
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate ratio as percentage
        return monthlyHousingCosts
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * Get historical housing costs by month for the last N months
     */
    public Map<String, BigDecimal> getHistoricalHousingCosts(final Long userId, final int months) {
        validateIdNotNull(userId);
        if (months <= 0) {
            throw new IllegalArgumentException("Number of months must be positive");
        }

        User user = getUserById(userId);
        Map<String, BigDecimal> historicalCosts = new java.util.LinkedHashMap<>();

        LocalDate endDate = LocalDate.now();

        for (int i = 0; i < months; i++) {
            LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            List<HousingExpenseDTO> expensesForMonth = housingExpenseRepository
                    .findActiveByUserAndDateRange(user, monthStart, monthEnd).stream()
                    .map(housingExpenseMapper::toDto)
                    .map(this::enrichWithCalculatedFields)
                    .toList();

            BigDecimal monthlyTotal = expensesForMonth.stream()
                    .map(HousingExpenseDTO::getMonthlyEquivalent)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String monthKey = monthStart.getYear() + "-" +
                    String.format("%02d", monthStart.getMonthValue());
            historicalCosts.put(monthKey, monthlyTotal);
        }

        return historicalCosts;
    }

    /**
     * Count active housing expenses for a user
     */
    public long countActiveExpenses(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return housingExpenseRepository.countByUserAndIsActiveTrue(user);
    }

    // Private helper methods

    /**
     * Enrich DTO with calculated fields
     */
    private HousingExpenseDTO enrichWithCalculatedFields(final HousingExpenseDTO dto) {
        if (dto == null || dto.getAmount() == null || dto.getFrequency() == null) {
            return dto;
        }

        // Calculate monthly and annual equivalents
        dto.setMonthlyEquivalent(dto.getFrequency().toMonthlyEquivalent(dto.getAmount()));
        dto.setAnnualEquivalent(dto.getFrequency().toAnnualEquivalent(dto.getAmount()));

        return dto;
    }

    /**
     * Calculate total monthly income for a user
     */
    private BigDecimal calculateMonthlyIncome(final User user) {
        List<IncomeSource> activeSources = incomeSourceRepository
                .findByUserAndIsActiveOrderByStartDateDesc(user, true);

        return activeSources.stream()
                .map(source -> {
                    if (source.getRecurrenceFrequency() != null && source.getNetAmount() != null) {
                        // Convert to monthly based on frequency
                        BigDecimal amount = source.getNetAmount();
                        return switch (source.getRecurrenceFrequency()) {
                            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52))
                                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                            case BI_WEEKLY -> amount.multiply(BigDecimal.valueOf(26))
                                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                            case MONTHLY -> amount;
                            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                            case SEMI_ANNUALLY -> amount.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
                            case ANNUALLY -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                            default -> BigDecimal.ZERO;
                        };
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getUserById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
    }

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    private void validateDtoNotNull(final HousingExpenseDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("HousingExpenseDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final HousingExpenseDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getExpenseType() == null) {
            throw new IllegalArgumentException("Expense type must not be null");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getFrequency() == null) {
            throw new IllegalArgumentException("Frequency must not be null");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
    }

    private void validateAmount(final HousingExpenseDTO dto) {
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
    }

    private void validateDates(final HousingExpenseDTO dto) {
        if (dto.getEndDate() != null && dto.getStartDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new IllegalArgumentException("End date must not be before start date");
            }
        }
    }
}
