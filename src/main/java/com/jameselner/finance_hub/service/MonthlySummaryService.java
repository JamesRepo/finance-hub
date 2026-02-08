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
import com.jameselner.finance_hub.util.FinancialThresholds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final HolidayService holidayService;

    private record ExpenseBreakdown(
            BigDecimal transactionExpenses,
            BigDecimal housingCosts,
            BigDecimal debtPayments,
            BigDecimal subscriptionCosts,
            BigDecimal holidayCosts,
            BigDecimal total
    ) {}

    private record BudgetPerformance(
            BigDecimal totalBudgeted,
            BigDecimal totalSpent,
            BigDecimal remaining,
            BigDecimal utilization,
            int overCount,
            int onTrackCount
    ) {}

    private record TransactionStatistics(
            int count,
            BigDecimal averageExpenseSize,
            BigDecimal largestExpense,
            String largestExpenseCategory
    ) {}

    /**
     * Generate comprehensive monthly summary for a user
     */
    public MonthlySummaryDTO generateMonthlySummary(final Long userId, final YearMonth month) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(month, "Month must not be null");

        User user = getUserById(userId);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // Fetch transactions once — reused by categories and statistics
        List<Transaction> transactions = transactionRepository.findByAccountUserAndTransactionDateBetween(
                user, startDate, endDate);

        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        ExpenseBreakdown expenses = calculateExpenseBreakdown(user, userId, month);
        BigDecimal netSavings = totalIncome.subtract(expenses.total());
        BigDecimal savingsRate = calculateSavingsRate(totalIncome, netSavings);

        List<BudgetDTO> budgets = budgetService.findByUserIdAndDateRange(userId, startDate, endDate);
        BudgetPerformance budgetPerf = calculateBudgetPerformance(budgets);

        // Variable spending categories (transaction-based)
        List<CategorySpendingDTO> variableCategories = calculateSpendingCategories(
                transactions, expenses.total(), budgets);

        // Fixed/recurring cost categories (synthetic)
        List<CategorySpendingDTO> fixedCategories = buildSyntheticCategories(expenses);

        // Combined sorted list for backwards compatibility
        List<CategorySpendingDTO> topCategories = Stream.concat(
                        variableCategories.stream(), fixedCategories.stream())
                .sorted(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed())
                .collect(Collectors.toList());

        BigDecimal housingRatio = housingExpenseService.calculateHousingToIncomeRatioForYear(userId, month.getYear());
        TransactionStatistics stats = calculateTransactionStatistics(transactions);

        MonthComparisonDTO monthComparison = calculateMonthComparison(
                user, userId, month, totalIncome, expenses.total(), netSavings, savingsRate);

        return MonthlySummaryDTO.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(expenses.total())
                .transactionExpenses(expenses.transactionExpenses())
                .netSavings(netSavings)
                .savingsRate(savingsRate)
                .totalBudgeted(budgetPerf.totalBudgeted())
                .totalSpent(budgetPerf.totalSpent())
                .budgetRemaining(budgetPerf.remaining())
                .budgetUtilization(budgetPerf.utilization())
                .budgetsOverCount(budgetPerf.overCount())
                .budgetsOnTrackCount(budgetPerf.onTrackCount())
                .topSpendingCategories(topCategories)
                .variableSpendingCategories(variableCategories)
                .fixedCostCategories(fixedCategories)
                .housingCosts(expenses.housingCosts())
                .holidayCosts(expenses.holidayCosts())
                .housingToIncomeRatio(housingRatio)
                .transactionCount(stats.count())
                .averageTransactionSize(stats.averageExpenseSize())
                .largestExpense(stats.largestExpense())
                .largestExpenseCategory(stats.largestExpenseCategory())
                .monthComparison(monthComparison)
                .build();
    }

    /**
     * Get the earliest month with transaction data for a user
     */
    public YearMonth getEarliestAvailableMonth(final Long userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        User user = getUserById(userId);
        return transactionRepository.findEarliestTransactionDateByUser(user)
                .map(YearMonth::from)
                .orElse(YearMonth.now());
    }

    private ExpenseBreakdown calculateExpenseBreakdown(final User user, final Long userId, final YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        BigDecimal transactionExpenses = calculateTransactionExpenses(user, startDate, endDate);
        BigDecimal housingCosts = housingExpenseService.calculateTotalForMonth(userId, startDate);
        BigDecimal debtPayments = debtPaymentService.calculateTotalForMonth(userId, startDate);
        BigDecimal subscriptionCosts = subscriptionService.calculateTotalForMonth(userId, month.getYear(), month.getMonthValue());
        BigDecimal holidayCosts = holidayService.calculateTotalForMonth(userId, startDate);
        BigDecimal total = transactionExpenses
                .add(housingCosts)
                .add(debtPayments)
                .add(subscriptionCosts)
                .add(holidayCosts);

        return new ExpenseBreakdown(transactionExpenses, housingCosts, debtPayments, subscriptionCosts, holidayCosts, total);
    }

    private BudgetPerformance calculateBudgetPerformance(final List<BudgetDTO> budgets) {
        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetDTO::getSpent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalBudgeted.subtract(totalSpent);
        BigDecimal utilization = calculatePercentage(totalSpent, totalBudgeted);

        int overCount = (int) budgets.stream()
                .filter(b -> b.getOverage() != null && b.getOverage().compareTo(BigDecimal.ZERO) > 0)
                .count();
        int onTrackCount = (int) budgets.stream()
                .filter(b -> b.getOverage() == null || b.getOverage().compareTo(BigDecimal.ZERO) <= 0)
                .count();

        return new BudgetPerformance(totalBudgeted, totalSpent, remaining, utilization, overCount, onTrackCount);
    }

    private TransactionStatistics calculateTransactionStatistics(final List<Transaction> transactions) {
        int count = transactions.size();
        BigDecimal averageExpenseSize = calculateAverageTransactionSize(transactions);

        Transaction largestExpenseTransaction = findLargestExpense(transactions);
        BigDecimal largestExpense = largestExpenseTransaction != null ?
                largestExpenseTransaction.getAmount() : BigDecimal.ZERO;
        String largestExpenseCategory = largestExpenseTransaction != null &&
                largestExpenseTransaction.getCategory() != null ?
                largestExpenseTransaction.getCategory().getCategoryName() : "N/A";

        return new TransactionStatistics(count, averageExpenseSize, largestExpense, largestExpenseCategory);
    }

    private List<CategorySpendingDTO> buildSyntheticCategories(final ExpenseBreakdown expenses) {
        List<CategorySpendingDTO> categories = new ArrayList<>();

        if (expenses.housingCosts().compareTo(FinancialThresholds.MINIMUM_CATEGORY_AMOUNT) >= 0) {
            categories.add(buildSyntheticCategory(
                    FinancialThresholds.HOUSING_CATEGORY_ID, "Housing",
                    FinancialThresholds.HOUSING_CATEGORY_COLOR, expenses.housingCosts(), expenses.total()));
        }
        if (expenses.debtPayments().compareTo(FinancialThresholds.MINIMUM_CATEGORY_AMOUNT) >= 0) {
            categories.add(buildSyntheticCategory(
                    FinancialThresholds.DEBT_CATEGORY_ID, "Debt Payments",
                    FinancialThresholds.DEBT_CATEGORY_COLOR, expenses.debtPayments(), expenses.total()));
        }
        if (expenses.subscriptionCosts().compareTo(FinancialThresholds.MINIMUM_CATEGORY_AMOUNT) >= 0) {
            categories.add(buildSyntheticCategory(
                    FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_NAME,
                    FinancialThresholds.SUBSCRIPTIONS_CATEGORY_COLOR, expenses.subscriptionCosts(), expenses.total()));
        }
        if (expenses.holidayCosts().compareTo(FinancialThresholds.MINIMUM_CATEGORY_AMOUNT) >= 0) {
            categories.add(buildSyntheticCategory(
                    FinancialThresholds.HOLIDAYS_CATEGORY_ID, FinancialThresholds.HOLIDAYS_CATEGORY_NAME,
                    FinancialThresholds.HOLIDAYS_CATEGORY_COLOR, expenses.holidayCosts(), expenses.total()));
        }

        categories.sort(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed());
        return categories;
    }

    private CategorySpendingDTO buildSyntheticCategory(
            final Long id, final String name, final String color,
            final BigDecimal amount, final BigDecimal totalExpenses) {
        return CategorySpendingDTO.builder()
                .categoryId(id)
                .categoryName(name)
                .categoryColor(color)
                .totalSpent(amount)
                .budgetAmount(null)
                .percentageOfTotal(calculatePercentage(amount, totalExpenses))
                .transactionCount(0)
                .build();
    }

    /**
     * Calculate total income for a period using IncomeSource data
     */
    private BigDecimal calculateTotalIncome(final User user, final LocalDate startDate, final LocalDate endDate) {
        List<IncomeSource> incomeSources = incomeSourceRepository.findByUserAndMonthRange(user, startDate, endDate);

        return incomeSources.stream()
                .map(source -> {
                    BigDecimal net = source.getNetAmount();
                    BigDecimal gross = source.getGrossAmount();
                    if (net != null) {
                        return net;
                    }
                    if (gross != null) {
                        return gross;
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTransactionExpenses(final User user, final LocalDate startDate, final LocalDate endDate) {
        BigDecimal expenses = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, startDate, endDate);
        return expenses != null ? expenses : BigDecimal.ZERO;
    }

    private BigDecimal calculateSavingsRate(final BigDecimal totalIncome, final BigDecimal netSavings) {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netSavings.multiply(BigDecimal.valueOf(100))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate spending categories from pre-fetched transactions (fix: no duplicate DB query)
     */
    private List<CategorySpendingDTO> calculateSpendingCategories(
            final List<Transaction> transactions,
            final BigDecimal totalExpenses,
            final List<BudgetDTO> budgets
    ) {
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .toList();

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

                    BigDecimal budgetAmount = budgets.stream()
                            .filter(b -> b.getCategoryId().equals(category.getCategoryId()))
                            .findFirst()
                            .map(BudgetDTO::getAmount)
                            .orElse(null);

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
                .filter(c -> c.getTotalSpent().compareTo(FinancialThresholds.MINIMUM_CATEGORY_AMOUNT) >= 0)
                .sorted(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calculate average transaction size for expenses only (fix: was averaging all transaction types)
     */
    private BigDecimal calculateAverageTransactionSize(final List<Transaction> transactions) {
        List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .toList();

        if (expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = expenses.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
    }

    private Transaction findLargestExpense(final List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount))
                .orElse(null);
    }

    /**
     * Calculate month-over-month comparison using ExpenseBreakdown for previous month
     */
    private MonthComparisonDTO calculateMonthComparison(
            final User user,
            final Long userId,
            final YearMonth currentMonth,
            final BigDecimal currentIncome,
            final BigDecimal currentExpenses,
            final BigDecimal currentSavings,
            final BigDecimal currentSavingsRate
    ) {
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        BigDecimal previousIncome = calculateTotalIncome(user, previousStart, previousEnd);
        ExpenseBreakdown previousBreakdown = calculateExpenseBreakdown(user, userId, previousMonth);
        BigDecimal previousExpenses = previousBreakdown.total();
        BigDecimal previousSavings = previousIncome.subtract(previousExpenses);
        BigDecimal previousSavingsRate = calculateSavingsRate(previousIncome, previousSavings);

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

    private BigDecimal calculatePercentage(final BigDecimal part, final BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP);
    }

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
