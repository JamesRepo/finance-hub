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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

        // Ensure expense month is set to first of month
        if (dto.getExpenseMonth() != null) {
            dto.setExpenseMonth(dto.getExpenseMonth().withDayOfMonth(1));
        }

        HousingExpense expense = housingExpenseMapper.toEntity(dto);
        HousingExpense savedExpense = housingExpenseRepository.save(expense);

        return housingExpenseMapper.toDto(savedExpense);
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

        // Ensure expense month is set to first of month
        if (dto.getExpenseMonth() != null) {
            dto.setExpenseMonth(dto.getExpenseMonth().withDayOfMonth(1));
        }

        housingExpenseMapper.updateEntityFromDto(existingExpense, dto);
        HousingExpense savedExpense = housingExpenseRepository.save(existingExpense);

        return housingExpenseMapper.toDto(savedExpense);
    }

    /**
     * Find housing expense by ID as DTO
     */
    public Optional<HousingExpenseDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return housingExpenseRepository.findById(id)
                .map(housingExpenseMapper::toDto);
    }

    /**
     * Find all housing expenses for a user
     */
    public List<HousingExpenseDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserOrderByExpenseMonthDesc(user).stream()
                .map(housingExpenseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find housing expenses for a specific month
     */
    public List<HousingExpenseDTO> findByUserIdAndMonthAsDto(final Long userId, final LocalDate month) {
        validateIdNotNull(userId);
        Objects.requireNonNull(month, "Month must not be null");
        User user = getUserById(userId);

        LocalDate firstOfMonth = month.withDayOfMonth(1);
        return housingExpenseRepository.findByUserAndExpenseMonthOrderByExpenseTypeAsc(user, firstOfMonth).stream()
                .map(housingExpenseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find housing expenses by user and type
     */
    public List<HousingExpenseDTO> findByUserIdAndTypeAsDto(final Long userId, final HousingExpenseType type) {
        validateIdNotNull(userId);
        Objects.requireNonNull(type, "Expense type must not be null");
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserAndExpenseTypeOrderByExpenseMonthDesc(user, type).stream()
                .map(housingExpenseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find housing expenses within a month range
     */
    public List<HousingExpenseDTO> findByUserIdAndMonthRangeAsDto(
            final Long userId,
            final LocalDate startMonth,
            final LocalDate endMonth) {
        validateIdNotNull(userId);
        Objects.requireNonNull(startMonth, "Start month must not be null");
        Objects.requireNonNull(endMonth, "End month must not be null");
        User user = getUserById(userId);

        return housingExpenseRepository.findByUserAndMonthRange(user, startMonth.withDayOfMonth(1), endMonth.withDayOfMonth(1)).stream()
                .map(housingExpenseMapper::toDto)
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
     * Copy all expenses from one month to the next month
     */
    @Transactional
    public List<HousingExpenseDTO> copyMonthToNext(final Long userId, final LocalDate sourceMonth) {
        validateIdNotNull(userId);
        Objects.requireNonNull(sourceMonth, "Source month must not be null");

        LocalDate sourceMonthFirst = sourceMonth.withDayOfMonth(1);
        LocalDate targetMonth = sourceMonthFirst.plusMonths(1);

        List<HousingExpenseDTO> sourceExpenses = findByUserIdAndMonthAsDto(userId, sourceMonthFirst);
        List<HousingExpenseDTO> copiedExpenses = new ArrayList<>();

        for (HousingExpenseDTO source : sourceExpenses) {
            HousingExpenseDTO copy = HousingExpenseDTO.builder()
                    .userId(userId)
                    .expenseType(source.getExpenseType())
                    .amount(source.getAmount())
                    .expenseMonth(targetMonth)
                    .build();

            HousingExpenseDTO saved = createFromDto(copy);
            copiedExpenses.add(saved);
        }

        log.info("Copied {} expenses from {} to {}", copiedExpenses.size(),
                sourceMonthFirst.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));

        return copiedExpenses;
    }

    /**
     * Get all distinct months that have expenses
     */
    public List<LocalDate> getDistinctMonths(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return housingExpenseRepository.findDistinctMonthsByUser(user);
    }

    /**
     * Calculate total for a specific month
     */
    public BigDecimal calculateTotalForMonth(final Long userId, final LocalDate month) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        LocalDate firstOfMonth = month.withDayOfMonth(1);
        return housingExpenseRepository.getTotalForMonth(user, firstOfMonth);
    }

    /**
     * Calculate total monthly housing costs (current month)
     */
    public BigDecimal calculateTotalMonthlyHousingCosts(final Long userId) {
        return calculateTotalForMonth(userId, LocalDate.now());
    }

    /**
     * Calculate total annual housing costs (sum of last 12 months or current month * 12)
     */
    public BigDecimal calculateTotalAnnualHousingCosts(final Long userId) {
        BigDecimal currentMonthTotal = calculateTotalMonthlyHousingCosts(userId);
        return currentMonthTotal.multiply(BigDecimal.valueOf(12));
    }

    /**
     * Get expenses grouped by month
     */
    public Map<String, List<HousingExpenseDTO>> getExpensesGroupedByMonth(final Long userId) {
        validateIdNotNull(userId);
        List<HousingExpenseDTO> allExpenses = findByUserIdAsDto(userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, List<HousingExpenseDTO>> grouped = new LinkedHashMap<>();

        for (HousingExpenseDTO expense : allExpenses) {
            String monthKey = expense.getExpenseMonth().format(formatter);
            grouped.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(expense);
        }

        return grouped;
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
        Map<String, BigDecimal> historicalCosts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < months; i++) {
            LocalDate month = today.minusMonths(i).withDayOfMonth(1);
            BigDecimal total = housingExpenseRepository.getTotalForMonth(user, month);

            String monthKey = month.getYear() + "-" + String.format("%02d", month.getMonthValue());
            historicalCosts.put(monthKey, total);
        }

        return historicalCosts;
    }

    /**
     * Count housing expenses for a user
     */
    public long countExpenses(final Long userId) {
        validateIdNotNull(userId);
        User user = getUserById(userId);
        return housingExpenseRepository.countByUser(user);
    }

    /**
     * Count expenses for current month
     */
    public long countExpensesForCurrentMonth(final Long userId) {
        List<HousingExpenseDTO> currentMonthExpenses = findByUserIdAndMonthAsDto(userId, LocalDate.now());
        return currentMonthExpenses.size();
    }

    /**
     * Calculate total housing expenses for a specific year
     */
    public BigDecimal calculateTotalForYear(final Long userId, final int year) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 1);

        List<HousingExpense> expenses = housingExpenseRepository.findByUserAndMonthRange(user, startOfYear, endOfYear);

        return expenses.stream()
                .map(HousingExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate average monthly housing expenses for a specific year
     */
    public BigDecimal calculateAverageMonthlyForYear(final Long userId, final int year) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 1);

        List<HousingExpense> expenses = housingExpenseRepository.findByUserAndMonthRange(user, startOfYear, endOfYear);

        if (expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Count distinct months that have expenses
        long monthsWithExpenses = expenses.stream()
                .map(HousingExpense::getExpenseMonth)
                .distinct()
                .count();

        if (monthsWithExpenses == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = expenses.stream()
                .map(HousingExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(monthsWithExpenses), 2, RoundingMode.HALF_UP);
    }

    /**
     * Count distinct months with expenses for a specific year
     */
    public long countMonthsWithExpensesForYear(final Long userId, final int year) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 1);

        List<HousingExpense> expenses = housingExpenseRepository.findByUserAndMonthRange(user, startOfYear, endOfYear);

        return expenses.stream()
                .map(HousingExpense::getExpenseMonth)
                .distinct()
                .count();
    }

    /**
     * Calculate housing-to-income ratio as a percentage based on total housing costs
     * and total net income (matching Take-Home Income calculation from Income Tracking)
     */
    public BigDecimal calculateHousingToIncomeRatioForYear(final Long userId, final int year) {
        validateIdNotNull(userId);
        User user = getUserById(userId);

        BigDecimal yearlyHousing = calculateTotalForYear(userId, year);
        BigDecimal totalNetIncome = getTakeHomeIncomeForYear(user, year);

        if (totalNetIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return yearlyHousing
                .multiply(BigDecimal.valueOf(100))
                .divide(totalNetIncome, 1, RoundingMode.HALF_UP);
    }

    // Private helper methods

    /**
     * Get total take-home income matching the Income Tracking view calculation
     */
    private BigDecimal getTakeHomeIncomeForYear(final User user, final int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 1);

        List<IncomeSource> activeSources = incomeSourceRepository
                .findByUserAndMonthRange(user, startOfYear, endOfYear);

        return activeSources.stream()
                .map(source -> source.getNetAmount() != null ? source.getNetAmount() : source.getGrossAmount())
                .filter(Objects::nonNull)
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
        if (dto.getExpenseMonth() == null) {
            throw new IllegalArgumentException("Expense month must not be null");
        }
    }

    private void validateAmount(final HousingExpenseDTO dto) {
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
    }
}
