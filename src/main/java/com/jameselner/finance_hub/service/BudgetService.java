package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Budget;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.Holiday;
import com.jameselner.finance_hub.domain.Subscription;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.util.FinancialThresholds;
import com.jameselner.finance_hub.mapper.BudgetMapper;
import com.jameselner.finance_hub.repository.BudgetRepository;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.HolidayExpenseRepository;
import com.jameselner.finance_hub.repository.HolidayRepository;
import com.jameselner.finance_hub.repository.SubscriptionRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final HolidayRepository holidayRepository;
    private final HolidayExpenseRepository holidayExpenseRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Create a new budget from DTO
     */
    @Transactional
    public BudgetDTO createBudgetFromDto(final BudgetDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getBudgetId() != null) {
            throw new IllegalArgumentException("Budget ID must be null for create operation");
        }

        validateDtoRequiredFields(dto);
        validateAmount(dto.getAmount());
        validateDates(dto.getStartDate(), dto.getEndDate());
        validateNoOverlappingBudgets(dto);

        Budget budget = budgetMapper.toEntity(dto);
        Budget savedBudget = budgetRepository.save(budget);
        return enrichWithCalculatedFields(budgetMapper.toDto(savedBudget));
    }

    /**
     * Update an existing budget from DTO
     */
    @Transactional
    public BudgetDTO updateBudgetFromDto(final BudgetDTO dto) {
        validateDtoNotNull(dto);

        if (dto.getBudgetId() == null) {
            throw new IllegalArgumentException("Budget ID must not be null for update operation");
        }

        Budget existingBudget = budgetRepository.findById(dto.getBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Budget with ID " + dto.getBudgetId() + " does not exist"));

        validateDtoRequiredFields(dto);
        validateAmount(dto.getAmount());
        validateDates(dto.getStartDate(), dto.getEndDate());
        validateNoOverlappingBudgetsForUpdate(dto);

        budgetMapper.updateEntityFromDto(existingBudget, dto);
        Budget savedBudget = budgetRepository.save(existingBudget);
        return enrichWithCalculatedFields(budgetMapper.toDto(savedBudget));
    }

    /**
     * Find a budget by ID and return as DTO with calculated fields
     */
    public Optional<BudgetDTO> findByIdAsDto(final Long id) {
        validateIdNotNull(id);
        return budgetRepository.findById(id)
                .map(budgetMapper::toDto)
                .map(this::enrichWithCalculatedFields);
    }

    /**
     * Find all budgets for a user and return as DTOs with calculated fields
     */
    public List<BudgetDTO> findByUserIdAsDto(final Long userId) {
        validateIdNotNull(userId);
        User user = findUserById(userId);

        return budgetRepository.findByUserOrderByStartDateDesc(user).stream()
                .map(budget -> enrichWithCalculatedFields(budgetMapper.toDto(budget), user, budget.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Find active budgets for a user on a specific date
     */
    public List<BudgetDTO> findActiveByUserIdAndDate(final Long userId, final LocalDate date) {
        validateIdNotNull(userId);
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }

        User user = findUserById(userId);

        return budgetRepository.findActiveByUserAndDate(user, date).stream()
                .map(budget -> enrichWithCalculatedFields(budgetMapper.toDto(budget), user, budget.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Find budgets for a user within a date range
     */
    public List<BudgetDTO> findByUserIdAndDateRange(
            final Long userId,
            final LocalDate startDate,
            final LocalDate endDate) {
        validateIdNotNull(userId);
        validateDates(startDate, endDate);

        User user = findUserById(userId);

        return budgetRepository.findByUserAndDateRange(user, startDate, endDate).stream()
                .map(budget -> enrichWithCalculatedFields(budgetMapper.toDto(budget), user, budget.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Delete a budget by ID
     */
    @Transactional
    public void deleteById(final Long id) {
        validateIdNotNull(id);

        if (!budgetRepository.existsById(id)) {
            throw new IllegalArgumentException("Budget with ID " + id + " does not exist");
        }

        budgetRepository.deleteById(id);
    }

    /**
     * Calculate budget usage based on transactions.
     * Looks up user and category from IDs — used by findByIdAsDto and create/update paths.
     */
    private BudgetDTO enrichWithCalculatedFields(final BudgetDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = findUserById(dto.getUserId());
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));

        return enrichWithCalculatedFields(dto, user, category);
    }

    /**
     * Calculate budget usage based on transactions.
     * Uses pre-fetched user and category to avoid redundant DB lookups.
     */
    private BudgetDTO enrichWithCalculatedFields(final BudgetDTO dto, final User user, final Category category) {
        if (dto == null) {
            return null;
        }

        BigDecimal spent = calculateSpentAmount(user, category, dto.getStartDate(), dto.getEndDate());
        dto.setSpent(spent);

        BigDecimal remaining = dto.getAmount().subtract(spent);
        BigDecimal overage = BigDecimal.ZERO;

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            overage = remaining.abs();
            remaining = BigDecimal.ZERO;
        }

        dto.setRemaining(remaining);
        dto.setOverage(overage);

        if (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            double percentage = spent.divide(dto.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
            dto.setPercentageUsed(percentage);
        } else {
            dto.setPercentageUsed(0.0);
        }

        return dto;
    }

    /**
     * Calculate the total amount spent for a category within a date range.
     * Public API that accepts IDs instead of entities.
     */
    public BigDecimal calculateSpentAmount(
            final Long userId,
            final Long categoryId,
            final LocalDate startDate,
            final LocalDate endDate) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID must not be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null");
        }

        User user = findUserById(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        return calculateSpentAmount(user, category, startDate, endDate);
    }

    /**
     * Calculate the total amount spent for a category within a date range.
     * For "Holidays" category, this includes both regular transactions and holiday expenses.
     * For "Subscriptions" category, this includes both regular transactions and subscription costs.
     */
    private BigDecimal calculateSpentAmount(
            final User user,
            final Category category,
            final LocalDate startDate,
            final LocalDate endDate) {
        // Use SUM query instead of fetching all transactions
        BigDecimal transactionTotal = transactionRepository.sumByUserAndCategoryAndTypeAndDateRange(
                user, category, TransactionType.EXPENSE, startDate, endDate);

        // If this is the "Holidays" category, also include holiday expenses
        BigDecimal holidayTotal = BigDecimal.ZERO;
        if (FinancialThresholds.HOLIDAYS_CATEGORY_NAME.equalsIgnoreCase(category.getCategoryName())) {
            List<Holiday> holidays = holidayRepository.findByUserAndDateRange(user, startDate, endDate);

            holidayTotal = holidays.stream()
                    .map(holidayExpenseRepository::calculateTotalSpent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // If this is the "Subscriptions" category, also include subscription costs
        BigDecimal subscriptionTotal = BigDecimal.ZERO;
        if (FinancialThresholds.SUBSCRIPTIONS_CATEGORY_NAME.equalsIgnoreCase(category.getCategoryName())) {
            List<Subscription> subscriptions = subscriptionRepository.findByUserAndPaymentDateBetween(
                    user, startDate, endDate);

            subscriptionTotal = subscriptions.stream()
                    .map(Subscription::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return transactionTotal.add(holidayTotal).add(subscriptionTotal);
    }

    /**
     * Get the earliest available month based on user's transaction history.
     * Falls back to 12 months ago if no transactions exist.
     */
    public YearMonth getEarliestAvailableMonth(final Long userId) {
        validateIdNotNull(userId);
        User user = findUserById(userId);

        return transactionRepository.findEarliestTransactionDateByUser(user)
                .map(YearMonth::from)
                .orElseGet(() -> YearMonth.now().minusMonths(12));
    }

    // Private validation methods

    private void validateIdNotNull(final Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Budget ID must not be null");
        }
    }

    private void validateAmount(final BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateDtoNotNull(final BudgetDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("BudgetDTO must not be null");
        }
    }

    private void validateDtoRequiredFields(final BudgetDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID must not be null");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (dto.getPeriodType() == null) {
            throw new IllegalArgumentException("Period type must not be null");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (dto.getEndDate() == null) {
            throw new IllegalArgumentException("End date must not be null");
        }
    }

    private void validateDates(final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    private void validateNoOverlappingBudgets(final BudgetDTO dto) {
        User user = findUserById(dto.getUserId());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));

        List<Budget> overlapping = budgetRepository.findOverlappingBudgets(
                user, category, dto.getStartDate(), dto.getEndDate());

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "A budget already exists for this category during the specified period");
        }
    }

    private void validateNoOverlappingBudgetsForUpdate(final BudgetDTO dto) {
        User user = findUserById(dto.getUserId());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dto.getCategoryId()));

        List<Budget> overlapping = budgetRepository.findOverlappingBudgets(
                user, category, dto.getStartDate(), dto.getEndDate());

        // Remove the current budget from the overlapping list
        overlapping = overlapping.stream()
                .filter(b -> !b.getBudgetId().equals(dto.getBudgetId()))
                .toList();

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "A budget already exists for this category during the specified period");
        }
    }

    private User findUserById(final Long userId) throws IllegalArgumentException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }
}
