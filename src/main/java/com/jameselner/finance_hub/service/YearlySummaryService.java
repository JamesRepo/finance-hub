package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthlyDataPointDTO;
import com.jameselner.finance_hub.dto.YearComparisonDTO;
import com.jameselner.finance_hub.dto.YearlySummaryDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YearlySummaryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final HousingExpenseService housingExpenseService;

    /**
     * Generate comprehensive yearly summary for a user
     */
    public YearlySummaryDTO generateYearlySummary(final Long userId, final Integer year) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(year, "Year must not be null");

        User user = getUserById(userId);
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // Calculate annual totals
        BigDecimal totalIncome = calculateTotalIncome(user, startDate, endDate);
        BigDecimal totalExpenses = calculateTotalExpenses(user, startDate, endDate);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = calculateSavingsRate(totalIncome, netSavings);

        // Generate monthly data points
        List<MonthlyDataPointDTO> monthlyData = generateMonthlyData(user, year);

        // Calculate averages
        BigDecimal averageMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal averageMonthlyExpenses = totalExpenses.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal averageMonthlySavings = netSavings.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // Find best/worst months
        MonthlyDataPointDTO highestIncomeMonth = monthlyData.stream()
                .max(Comparator.comparing(MonthlyDataPointDTO::getIncome))
                .orElse(null);

        MonthlyDataPointDTO highestExpenseMonth = monthlyData.stream()
                .max(Comparator.comparing(MonthlyDataPointDTO::getExpenses))
                .orElse(null);

        MonthlyDataPointDTO bestSavingsMonth = monthlyData.stream()
                .max(Comparator.comparing(MonthlyDataPointDTO::getSavings))
                .orElse(null);

        // Top spending categories
        List<CategorySpendingDTO> topCategories = calculateTopSpendingCategories(user, startDate, endDate, totalExpenses, 10);
        BigDecimal totalCategorySpending = topCategories.stream()
                .map(CategorySpendingDTO::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Budget performance
        List<BudgetDTO> yearBudgets = budgetService.findByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal totalBudgeted = yearBudgets.stream()
                .map(BudgetDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = yearBudgets.stream()
                .map(BudgetDTO::getSpent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetUtilization = calculatePercentage(totalSpent, totalBudgeted);

        // Housing and debt
        BigDecimal totalHousingCosts = calculateAnnualHousingCosts(userId);
        BigDecimal totalDebtPayments = BigDecimal.ZERO; // TODO: Implement when debt service is ready

        // Transaction statistics
        List<Transaction> yearTransactions = transactionRepository.findByAccountUserAndTransactionDateBetween(
                user, startDate, endDate);
        int totalTransactions = yearTransactions.size();

        Transaction largestTransaction = yearTransactions.stream()
                .max(Comparator.comparing(Transaction::getAmount))
                .orElse(null);
        BigDecimal largestTransactionAmount = largestTransaction != null ?
                largestTransaction.getAmount() : BigDecimal.ZERO;
        String largestTransactionCategory = largestTransaction != null &&
                largestTransaction.getCategory() != null ?
                largestTransaction.getCategory().getCategoryName() : "N/A";

        // Year-over-year comparison
        YearComparisonDTO yearComparison = calculateYearComparison(user, year);

        return YearlySummaryDTO.builder()
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .savingsRate(savingsRate)
                .monthlyData(monthlyData)
                .averageMonthlyIncome(averageMonthlyIncome)
                .averageMonthlyExpenses(averageMonthlyExpenses)
                .averageMonthlySavings(averageMonthlySavings)
                .highestIncomeMonth(highestIncomeMonth != null ?
                        highestIncomeMonth.getMonth().format(DateTimeFormatter.ofPattern("MMMM")) : "N/A")
                .highestIncomeAmount(highestIncomeMonth != null ? highestIncomeMonth.getIncome() : BigDecimal.ZERO)
                .highestExpenseMonth(highestExpenseMonth != null ?
                        highestExpenseMonth.getMonth().format(DateTimeFormatter.ofPattern("MMMM")) : "N/A")
                .highestExpenseAmount(highestExpenseMonth != null ? highestExpenseMonth.getExpenses() : BigDecimal.ZERO)
                .bestSavingsMonth(bestSavingsMonth != null ?
                        bestSavingsMonth.getMonth().format(DateTimeFormatter.ofPattern("MMMM")) : "N/A")
                .bestSavingsAmount(bestSavingsMonth != null ? bestSavingsMonth.getSavings() : BigDecimal.ZERO)
                .topSpendingCategories(topCategories)
                .totalCategorySpending(totalCategorySpending)
                .yearComparison(yearComparison)
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .budgetUtilization(budgetUtilization)
                .totalHousingCosts(totalHousingCosts)
                .totalDebtPayments(totalDebtPayments)
                .totalTransactions(totalTransactions)
                .largestTransaction(largestTransactionAmount)
                .largestTransactionCategory(largestTransactionCategory)
                .build();
    }

    /**
     * Generate monthly data points for the year
     */
    private List<MonthlyDataPointDTO> generateMonthlyData(final User user, final Integer year) {
        List<MonthlyDataPointDTO> monthlyData = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            BigDecimal income = calculateTotalIncome(user, startDate, endDate);
            BigDecimal expenses = calculateTotalExpenses(user, startDate, endDate);
            BigDecimal savings = income.subtract(expenses);
            BigDecimal savingsRate = calculateSavingsRate(income, savings);

            monthlyData.add(MonthlyDataPointDTO.builder()
                    .month(yearMonth)
                    .income(income)
                    .expenses(expenses)
                    .savings(savings)
                    .savingsRate(savingsRate)
                    .build());
        }

        return monthlyData;
    }

    /**
     * Calculate total income for a period
     */
    private BigDecimal calculateTotalIncome(final User user, final LocalDate startDate, final LocalDate endDate) {
        BigDecimal income = transactionRepository.sumByUserAndTypeAndDateRange(
                user, TransactionType.INCOME, startDate, endDate);
        return income != null ? income : BigDecimal.ZERO;
    }

    /**
     * Calculate total expenses for a period
     */
    private BigDecimal calculateTotalExpenses(final User user, final LocalDate startDate, final LocalDate endDate) {
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
     * Calculate top spending categories for the year
     */
    private List<CategorySpendingDTO> calculateTopSpendingCategories(
            final User user,
            final LocalDate startDate,
            final LocalDate endDate,
            final BigDecimal totalExpenses,
            final int limit
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

                    return CategorySpendingDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getCategoryName())
                            .categoryColor(category.getColorCode())
                            .totalSpent(categoryTotal)
                            .percentageOfTotal(percentageOfTotal)
                            .transactionCount(categoryTransactions.size())
                            .build();
                })
                .sorted(Comparator.comparing(CategorySpendingDTO::getTotalSpent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculate year-over-year comparison
     */
    private YearComparisonDTO calculateYearComparison(final User user, final Integer currentYear) {
        Integer previousYear = currentYear - 1;

        LocalDate currentStart = LocalDate.of(currentYear, 1, 1);
        LocalDate currentEnd = LocalDate.of(currentYear, 12, 31);
        LocalDate previousStart = LocalDate.of(previousYear, 1, 1);
        LocalDate previousEnd = LocalDate.of(previousYear, 12, 31);

        // Current year
        BigDecimal currentIncome = calculateTotalIncome(user, currentStart, currentEnd);
        BigDecimal currentExpenses = calculateTotalExpenses(user, currentStart, currentEnd);
        BigDecimal currentSavings = currentIncome.subtract(currentExpenses);
        BigDecimal currentSavingsRate = calculateSavingsRate(currentIncome, currentSavings);

        // Previous year
        BigDecimal previousIncome = calculateTotalIncome(user, previousStart, previousEnd);
        BigDecimal previousExpenses = calculateTotalExpenses(user, previousStart, previousEnd);
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

        return YearComparisonDTO.builder()
                .previousYear(previousYear)
                .currentYear(currentYear)
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
     * Calculate annual housing costs
     */
    private BigDecimal calculateAnnualHousingCosts(final Long userId) {
        BigDecimal monthlyHousingCosts = housingExpenseService.calculateTotalMonthlyHousingCosts(userId);
        return monthlyHousingCosts.multiply(BigDecimal.valueOf(12));
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
