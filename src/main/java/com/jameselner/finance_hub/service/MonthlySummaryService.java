package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthComparisonDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlySummaryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final BudgetService budgetService;
    private final HousingExpenseService housingExpenseService;
    private final DebtPaymentService debtPaymentService;
    private final SubscriptionService subscriptionService;

    /**
     * Generate comprehensive monthly summary for a user
     */
    public MonthlySummaryDTO generateMonthlySummary(final Long userId, final YearMonth month) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(month, "Month must not be null");

        User user = getUserById(userId);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // Calculate all metrics
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal transactionExpenses = calculateTransactionExpenses(user, startDate, endDate);
        BigDecimal housingCosts = housingExpenseService.calculateTotalForMonth(userId, startDate);
        BigDecimal debtPayments = debtPaymentService.calculateTotalForMonth(userId, startDate);
        BigDecimal subscriptionCosts = subscriptionService.calculateTotalForMonth(userId, month.getYear(), month.getMonthValue());
        BigDecimal totalExpenses = transactionExpenses
                .add(housingCosts)
                .add(debtPayments)
                .add(subscriptionCosts);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = calculateSavingsRate(totalIncome, netSavings);

        // Budget performance
        List<BudgetDTO> budgets = budgetService.findByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetDTO::getSpent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetRemaining = totalBudgeted.subtract(totalSpent);
        BigDecimal budgetUtilization = calculatePercentage(totalSpent, totalBudgeted);

        int budgetsOverCount = (int) budgets.stream()
                .filter(b -> b.getOverage() != null && b.getOverage().compareTo(BigDecimal.ZERO) > 0)
                .count();
        int budgetsOnTrackCount = (int) budgets.stream()
                .filter(b -> b.getOverage() == null || b.getOverage().compareTo(BigDecimal.ZERO) <= 0)
                .count();

        // All spending categories with at least £1 spent
        List<CategorySpendingDTO> topCategories = calculateSpendingCategories(user, startDate, endDate, totalExpenses);

        // Add Housing as a category if there are housing costs
        if (housingCosts.compareTo(BigDecimal.ONE) >= 0) {
            topCategories.add(CategorySpendingDTO.builder()
                    .categoryId(-1L)
                    .categoryName("Housing")
                    .categoryColor("#4A90A4")
                    .totalSpent(housingCosts)
                    .budgetAmount(null)
                    .percentageOfTotal(calculatePercentage(housingCosts, totalExpenses))
                    .transactionCount(0)
                    .build());
        }

        // Add Debt Payments as a category if there are debt payments
        if (debtPayments.compareTo(BigDecimal.ONE) >= 0) {
            topCategories.add(CategorySpendingDTO.builder()
                    .categoryId(-2L)
                    .categoryName("Debt Payments")
                    .categoryColor("#E57373")
                    .totalSpent(debtPayments)
                    .budgetAmount(null)
                    .percentageOfTotal(calculatePercentage(debtPayments, totalExpenses))
                    .transactionCount(0)
                    .build());
        }

        // Add Subscriptions as a category if there are subscription costs
        if (subscriptionCosts.compareTo(BigDecimal.ONE) >= 0) {
            topCategories.add(CategorySpendingDTO.builder()
                    .categoryId(-3L)
                    .categoryName("Subscriptions")
                    .categoryColor("#9575CD")
                    .totalSpent(subscriptionCosts)
                    .budgetAmount(null)
                    .percentageOfTotal(calculatePercentage(subscriptionCosts, totalExpenses))
                    .transactionCount(0)
                    .build());
        }

        // Re-sort by total spent
        topCategories.sort(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed());

        BigDecimal housingRatio = housingExpenseService.calculateHousingToIncomeRatioForYear(userId, LocalDate.now().getYear());

        // Transaction statistics
        List<Transaction> transactions = transactionRepository.findByAccountUserAndTransactionDateBetween(
                user, startDate, endDate);
        int transactionCount = transactions.size();
        BigDecimal averageTransactionSize = calculateAverageTransactionSize(transactions);

        Transaction largestExpenseTransaction = findLargestExpense(transactions);
        BigDecimal largestExpense = largestExpenseTransaction != null ?
                largestExpenseTransaction.getAmount() : BigDecimal.ZERO;
        String largestExpenseCategory = largestExpenseTransaction != null &&
                largestExpenseTransaction.getCategory() != null ?
                largestExpenseTransaction.getCategory().getCategoryName() : "N/A";

        // Month-over-month comparison
        MonthComparisonDTO monthComparison = calculateMonthComparison(user, month);

        return MonthlySummaryDTO.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .transactionExpenses(transactionExpenses)
                .netSavings(netSavings)
                .savingsRate(savingsRate)
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .budgetRemaining(budgetRemaining)
                .budgetUtilization(budgetUtilization)
                .budgetsOverCount(budgetsOverCount)
                .budgetsOnTrackCount(budgetsOnTrackCount)
                .topSpendingCategories(topCategories)
                .housingCosts(housingCosts)
                .housingToIncomeRatio(housingRatio)
                .transactionCount(transactionCount)
                .averageTransactionSize(averageTransactionSize)
                .largestExpense(largestExpense)
                .largestExpenseCategory(largestExpenseCategory)
                .monthComparison(monthComparison)
                .build();
    }

    /**
     * Calculate total income for a period using IncomeSource data
     */
    private BigDecimal calculateTotalIncome(final User user, final LocalDate startDate, final LocalDate endDate) {
        List<IncomeSource> incomeSources = incomeSourceRepository.findByUserAndMonthRange(user, startDate, endDate);

        return incomeSources.stream()
                .map(source -> source.getNetAmount() != null ? source.getNetAmount() : source.getGrossAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate transaction expenses for a period (excluding housing costs)
     */
    private BigDecimal calculateTransactionExpenses(final User user, final LocalDate startDate, final LocalDate endDate) {
        BigDecimal expenses = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, startDate, endDate);
        return expenses != null ? expenses : BigDecimal.ZERO;
    }

    /**
     * Calculate savings rate as percentage
     */
    private BigDecimal calculateSavingsRate(final BigDecimal totalIncome, final BigDecimal netSavings) {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netSavings.multiply(BigDecimal.valueOf(100))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate all spending categories with at least £1 spent
     */
    private List<CategorySpendingDTO> calculateSpendingCategories(
            final User user,
            final LocalDate startDate,
            final LocalDate endDate,
            final BigDecimal totalExpenses
    ) {
        List<Transaction> expenseTransactions = transactionRepository
                .findByAccountUserAndTransactionDateBetween(user, startDate, endDate)
                .stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .toList();

        // Group by category and calculate totals
        Map<Category, List<Transaction>> byCategory = expenseTransactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(Transaction::getCategory));

        return byCategory.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();
                    List<Transaction> categoryTransactions = entry.getValue();

                    BigDecimal categoryTotal = categoryTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentageOfTotal = calculatePercentage(categoryTotal, totalExpenses);

                    // Get budget for this category if exists
                    List<BudgetDTO> categoryBudgets = budgetService.findByUserIdAndDateRange(
                            user.getUserId(), startDate, endDate).stream()
                            .filter(b -> b.getCategoryId().equals(category.getCategoryId()))
                            .toList();

                    BigDecimal budgetAmount = categoryBudgets.isEmpty() ? null :
                            categoryBudgets.getFirst().getAmount();

                    return CategorySpendingDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getCategoryName())
                            .categoryColor(category.getColorCode())
                            .totalSpent(categoryTotal)
                            .budgetAmount(budgetAmount)
                            .percentageOfTotal(percentageOfTotal)
                            .transactionCount(categoryTransactions.size())
                            .build();
                })
                // Filter for categories with at least £1 spent
                .filter(c -> c.getTotalSpent().compareTo(BigDecimal.ONE) >= 0)
                .sorted(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calculate average transaction size
     */
    private BigDecimal calculateAverageTransactionSize(final List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Find largest expense transaction
     */
    private Transaction findLargestExpense(final List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount))
                .orElse(null);
    }

    /**
     * Calculate month-over-month comparison
     */
    private MonthComparisonDTO calculateMonthComparison(final User user, final YearMonth currentMonth) {
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDate currentStart = currentMonth.atDay(1);
        LocalDate currentEnd = currentMonth.atEndOfMonth();
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        Long userId = user.getUserId();

        // Current month (transaction expenses + housing + debt + subscriptions)
        BigDecimal currentIncome = calculateTotalIncome(user, currentStart, currentEnd);
        BigDecimal currentTransactionExpenses = calculateTransactionExpenses(user, currentStart, currentEnd);
        BigDecimal currentHousing = housingExpenseService.calculateTotalForMonth(userId, currentStart);
        BigDecimal currentDebt = debtPaymentService.calculateTotalForMonth(userId, currentStart);
        BigDecimal currentSubscriptions = subscriptionService.calculateTotalForMonth(userId, currentMonth.getYear(), currentMonth.getMonthValue());
        BigDecimal currentExpenses = currentTransactionExpenses.add(currentHousing).add(currentDebt).add(currentSubscriptions);
        BigDecimal currentSavings = currentIncome.subtract(currentExpenses);
        BigDecimal currentSavingsRate = calculateSavingsRate(currentIncome, currentSavings);

        // Previous month (transaction expenses + housing + debt + subscriptions)
        BigDecimal previousIncome = calculateTotalIncome(user, previousStart, previousEnd);
        BigDecimal previousTransactionExpenses = calculateTransactionExpenses(user, previousStart, previousEnd);
        BigDecimal previousHousing = housingExpenseService.calculateTotalForMonth(userId, previousStart);
        BigDecimal previousDebt = debtPaymentService.calculateTotalForMonth(userId, previousStart);
        BigDecimal previousSubscriptions = subscriptionService.calculateTotalForMonth(userId, previousMonth.getYear(), previousMonth.getMonthValue());
        BigDecimal previousExpenses = previousTransactionExpenses.add(previousHousing).add(previousDebt).add(previousSubscriptions);
        BigDecimal previousSavings = previousIncome.subtract(previousExpenses);
        BigDecimal previousSavingsRate = calculateSavingsRate(previousIncome, previousSavings);

        // Calculate changes
        BigDecimal incomeChange = currentIncome.subtract(previousIncome);
        BigDecimal incomeChangePercent = calculateChangePercentage(previousIncome, currentIncome);

        BigDecimal expenseChange = currentExpenses.subtract(previousExpenses);
        BigDecimal expenseChangePercent = calculateChangePercentage(previousExpenses, currentExpenses);

        BigDecimal savingsChange = currentSavings.subtract(previousSavings);
        BigDecimal savingsChangePercent = calculateChangePercentage(previousSavings, currentSavings);

        BigDecimal savingsRateChange = currentSavingsRate.subtract(previousSavingsRate);

        return MonthComparisonDTO.builder()
                .incomeChange(incomeChange)
                .incomeChangePercent(incomeChangePercent)
                .expenseChange(expenseChange)
                .expenseChangePercent(expenseChangePercent)
                .savingsChange(savingsChange)
                .savingsChangePercent(savingsChangePercent)
                .savingsRateChange(savingsRateChange)
                .build();
    }

    /**
     * Calculate percentage
     */
    private BigDecimal calculatePercentage(final BigDecimal part, final BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate change percentage
     */
    private BigDecimal calculateChangePercentage(final BigDecimal oldValue, final BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal change = newValue.subtract(oldValue);
        return change.multiply(BigDecimal.valueOf(100))
                .divide(oldValue, 2, RoundingMode.HALF_UP);
    }

    private User getUserById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
    }
}
