package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.dto.IncomeReportDTO;
import com.jameselner.finance_hub.dto.IncomeVsExpenseDTO;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeReportService {

    private final IncomeSourceRepository incomeSourceRepository;
    private final TransactionRepository transactionRepository;

    public IncomeReportDTO generateIncomeReport(final User user, final LocalDate startDate, final LocalDate endDate) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        List<IncomeSource> allIncomeSources = incomeSourceRepository.findByUserOrderByStartDateDesc(user);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal recurringIncome = BigDecimal.ZERO;
        BigDecimal oneTimeIncome = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeByCategory = new HashMap<>();
        Map<String, BigDecimal> incomeBySource = new HashMap<>();

        BigDecimal highestAmount = BigDecimal.ZERO;
        String highestSourceName = null;

        for (IncomeSource source : allIncomeSources) {
            if (source.getIsActive() && isSourceRelevantForPeriod(source, startDate, endDate)) {
                BigDecimal sourceAmount;

                if (source.getIsRecurring()) {
                    // Calculate total for recurring sources based on occurrences in period
                    int occurrences = countOccurrencesInPeriod(source, startDate, endDate);
                    sourceAmount = source.getAmount().multiply(BigDecimal.valueOf(occurrences));
                    recurringIncome = recurringIncome.add(sourceAmount);
                } else {
                    // One-time income: only count if within period
                    if (isDateWithinPeriod(source.getStartDate(), startDate, endDate)) {
                        sourceAmount = source.getAmount();
                        oneTimeIncome = oneTimeIncome.add(sourceAmount);
                    } else {
                        continue;
                    }
                }

                totalIncome = totalIncome.add(sourceAmount);

                if (source.getCategory() != null) {
                    String categoryName = source.getCategory().getCategoryName();
                    incomeByCategory.merge(categoryName, sourceAmount, BigDecimal::add);
                }

                incomeBySource.merge(source.getSourceName(), sourceAmount, BigDecimal::add);

                if (sourceAmount.compareTo(highestAmount) > 0) {
                    highestAmount = sourceAmount;
                    highestSourceName = source.getSourceName();
                }
            }
        }

        long activeSourcesCount = incomeSourceRepository.countActiveIncomeSourcesByUser(user);
        long recurringSourcesCount = incomeSourceRepository.countRecurringIncomeSourcesByUser(user);

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal averageMonthlyIncome = BigDecimal.ZERO;
        if (daysBetween > 0) {
            BigDecimal monthsInPeriod = BigDecimal.valueOf(daysBetween).divide(
                    BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            if (monthsInPeriod.compareTo(BigDecimal.ZERO) > 0) {
                averageMonthlyIncome = totalIncome.divide(monthsInPeriod, 2, RoundingMode.HALF_UP);
            }
        }

        return IncomeReportDTO.builder()
                .reportStartDate(startDate)
                .reportEndDate(endDate)
                .totalIncome(totalIncome)
                .recurringIncome(recurringIncome)
                .oneTimeIncome(oneTimeIncome)
                .averageMonthlyIncome(averageMonthlyIncome)
                .activeSourcesCount(activeSourcesCount)
                .recurringSourcesCount(recurringSourcesCount)
                .highestIncomeSource(highestAmount)
                .highestIncomeSourceName(highestSourceName)
                .incomeByCategory(incomeByCategory)
                .incomeBySource(incomeBySource)
                .build();
    }

    public IncomeVsExpenseDTO compareIncomeVsExpenses(
            final User user,
            final LocalDate startDate,
            final LocalDate endDate) {

        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Use income sources to calculate expected income for the period
        // This accounts for recurring income properly
        BigDecimal totalIncome = calculateTotalIncomeForPeriod(user, startDate, endDate);

        // Get transactions for expenses only (don't double-count income from transactions)
        List<Transaction> transactions = transactionRepository.findByAccountUserAndTransactionDateBetween(
                user, startDate, endDate);

        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getCategory() != null &&
                    transaction.getCategory().getCategoryType() == CategoryType.EXPENSE) {
                totalExpenses = totalExpenses.add(transaction.getAmount());
            }
            // Note: We don't add income transactions here as income is calculated from income sources
        }

        BigDecimal netAmount = totalIncome.subtract(totalExpenses);
        boolean isPositive = netAmount.compareTo(BigDecimal.ZERO) >= 0;

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netAmount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        String status;
        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0) {
                status = "EXCELLENT";
            } else if (savingsRate.compareTo(BigDecimal.valueOf(10)) >= 0) {
                status = "GOOD";
            } else {
                status = "MODERATE";
            }
        } else if (netAmount.compareTo(BigDecimal.ZERO) == 0) {
            status = "BREAK_EVEN";
        } else {
            status = "DEFICIT";
        }

        return IncomeVsExpenseDTO.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netAmount(netAmount)
                .savingsRate(savingsRate)
                .isPositive(isPositive)
                .status(status)
                .build();
    }

    public IncomeVsExpenseDTO compareCurrentMonth(final User user) {
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        return compareIncomeVsExpenses(user, startDate, endDate);
    }

    public IncomeVsExpenseDTO compareLastMonth(final User user) {
        LocalDate endDate = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate startDate = endDate.withDayOfMonth(1);
        return compareIncomeVsExpenses(user, startDate, endDate);
    }

    public IncomeVsExpenseDTO compareYearToDate(final User user) {
        LocalDate startDate = LocalDate.now().withDayOfYear(1);
        LocalDate endDate = LocalDate.now();
        return compareIncomeVsExpenses(user, startDate, endDate);
    }

    private BigDecimal calculateTotalIncomeForPeriod(final User user, final LocalDate startDate, final LocalDate endDate) {
        List<IncomeSource> activeSources = incomeSourceRepository.findByUserAndIsActiveOrderByStartDateDesc(user, true);
        BigDecimal totalIncome = BigDecimal.ZERO;

        for (IncomeSource source : activeSources) {
            if (isSourceRelevantForPeriod(source, startDate, endDate)) {
                if (source.getIsRecurring()) {
                    int occurrences = countOccurrencesInPeriod(source, startDate, endDate);
                    totalIncome = totalIncome.add(source.getAmount().multiply(BigDecimal.valueOf(occurrences)));
                } else {
                    if (isDateWithinPeriod(source.getStartDate(), startDate, endDate)) {
                        totalIncome = totalIncome.add(source.getAmount());
                    }
                }
            }
        }

        return totalIncome;
    }

    private boolean isSourceRelevantForPeriod(final IncomeSource source, final LocalDate startDate, final LocalDate endDate) {
        if (!source.getIsRecurring()) {
            return isDateWithinPeriod(source.getStartDate(), startDate, endDate);
        }

        LocalDate sourceEndDate = source.getEndDate() != null ? source.getEndDate() : endDate;
        return !source.getStartDate().isAfter(endDate) && !sourceEndDate.isBefore(startDate);
    }

    private int countOccurrencesInPeriod(final IncomeSource source, final LocalDate startDate, final LocalDate endDate) {
        if (!source.getIsRecurring() || source.getRecurrenceFrequency() == null) {
            return 0;
        }

        int count = 0;
        LocalDate currentDate = source.getNextExpectedDate() != null ?
                source.getNextExpectedDate() : source.getStartDate();

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            if (isDateWithinPeriod(currentDate, startDate, endDate)) {
                if (source.getEndDate() == null || currentDate.isBefore(source.getEndDate()) ||
                        currentDate.isEqual(source.getEndDate())) {
                    count++;
                }
            }

            currentDate = switch (source.getRecurrenceFrequency()) {
                case ONE_TIME -> null;
                case WEEKLY -> currentDate.plusWeeks(1);
                case BI_WEEKLY -> currentDate.plusWeeks(2);
                case MONTHLY -> currentDate.plusMonths(1);
                case QUARTERLY -> currentDate.plusMonths(3);
                case SEMI_ANNUALLY -> currentDate.plusMonths(6);
                case ANNUALLY -> currentDate.plusYears(1);
            };

            if (currentDate == null) {
                break;
            }
        }

        return count;
    }

    private boolean isDateWithinPeriod(final LocalDate date, final LocalDate startDate, final LocalDate endDate) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
                (date.isEqual(endDate) || date.isBefore(endDate));
    }
}
