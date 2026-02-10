package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO.CategoryProjection;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnualProjectionService {

    private final BudgetService budgetService;
    private final HousingExpenseService housingExpenseService;
    private final DebtService debtService;
    private final SubscriptionService subscriptionService;
    private final HolidayService holidayService;
    private final MonthlySummaryService monthlySummaryService;
    private final UserRepository userRepository;

    /**
     * Generate an annual financial projection for the given year.
     */
    public AnnualProjectionDTO generateProjection(final Long userId, final Integer year) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(year, "Year must not be null");

        User user = getUserById(userId);
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // Project income (current month actual × 12, using net amounts)
        BigDecimal projectedIncome = projectIncome(userId);

        // Project expenses from budgets
        List<BudgetDTO> budgets = budgetService.findByUserIdAndDateRange(userId, yearStart, yearEnd);
        Map<String, CategoryProjection> budgetCategories = annualizeBudgets(budgets, yearStart, yearEnd);
        BigDecimal budgetedExpenses = budgetCategories.values().stream()
                .map(CategoryProjection::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Project pseudo-category expenses
        BigDecimal housingCosts = housingExpenseService.calculateTotalAnnualHousingCosts(userId);
        BigDecimal subscriptionCosts = projectSubscriptionCosts(userId);
        BigDecimal debtPayments = projectDebtPayments(user);
        BigDecimal holidayCosts = projectHolidayCosts(userId, yearStart, yearEnd);

        BigDecimal projectedExpenses = budgetedExpenses
                .add(housingCosts)
                .add(debtPayments)
                .add(subscriptionCosts)
                .add(holidayCosts);

        BigDecimal projectedNetSavings = projectedIncome.subtract(projectedExpenses);
        BigDecimal projectedSavingsRate = calculateSavingsRate(projectedIncome, projectedNetSavings);

        // Build category projections
        List<CategoryProjection> categoryProjections = buildCategoryProjections(
                budgetCategories, housingCosts, debtPayments, subscriptionCosts, holidayCosts, projectedExpenses);

        AnnualProjectionDTO.AnnualProjectionDTOBuilder builder = AnnualProjectionDTO.builder()
                .year(year)
                .projectedIncome(projectedIncome)
                .projectedExpenses(projectedExpenses)
                .projectedNetSavings(projectedNetSavings)
                .projectedSavingsRate(projectedSavingsRate)
                .budgetedExpenses(budgetedExpenses)
                .housingCosts(housingCosts)
                .debtPayments(debtPayments)
                .subscriptionCosts(subscriptionCosts)
                .holidayCosts(holidayCosts)
                .categoryProjections(categoryProjections);

        // Build blended view for current year
        int currentYear = LocalDate.now().getYear();
        if (year == currentYear) {
            populateBlendedView(builder, userId, user, year, projectedIncome, projectedExpenses);
        }

        return builder.build();
    }

    /**
     * Project income for the year using the most recent completed month's actual income × 12.
     * This uses net income (via MonthlySummaryService) rather than gross, matching what the user
     * sees in their monthly summaries.
     */
    BigDecimal projectIncome(final Long userId) {
        YearMonth lastCompletedMonth = YearMonth.now().minusMonths(1);
        MonthlySummaryDTO summary = monthlySummaryService.generateMonthlySummary(userId, lastCompletedMonth);
        return summary.getTotalIncome().multiply(BigDecimal.valueOf(12));
    }

    /**
     * Annualize budgets based on their PeriodType and overlap with the target year.
     * Returns a map keyed by category name for deduplication.
     */
    Map<String, CategoryProjection> annualizeBudgets(
            final List<BudgetDTO> budgets,
            final LocalDate yearStart,
            final LocalDate yearEnd) {

        Map<String, CategoryProjection> categoryMap = new LinkedHashMap<>();

        for (BudgetDTO budget : budgets) {
            // Skip budgets for pseudo-categories — these are projected separately
            // from their dedicated data sources (housing, debt, subscriptions, holidays)
            if (isSyntheticCategory(budget.getCategoryName())) {
                continue;
            }
            BigDecimal annualized = annualizeBudget(budget, yearStart, yearEnd);
            if (annualized.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String key = budget.getCategoryName();
            CategoryProjection existing = categoryMap.get(key);
            if (existing != null) {
                categoryMap.put(key, CategoryProjection.builder()
                        .categoryId(existing.getCategoryId())
                        .categoryName(existing.getCategoryName())
                        .colorCode(existing.getColorCode())
                        .annualAmount(existing.getAnnualAmount().add(annualized))
                        .periodType(budget.getPeriodType().name())
                        .synthetic(false)
                        .build());
            } else {
                categoryMap.put(key, CategoryProjection.builder()
                        .categoryId(budget.getCategoryId())
                        .categoryName(budget.getCategoryName())
                        .colorCode(budget.getCategoryColorCode())
                        .annualAmount(annualized)
                        .periodType(budget.getPeriodType().name())
                        .synthetic(false)
                        .build());
            }
        }

        return categoryMap;
    }

    /**
     * Annualize a single budget based on its PeriodType and date overlap with the target year.
     */
    BigDecimal annualizeBudget(final BudgetDTO budget, final LocalDate yearStart, final LocalDate yearEnd) {
        LocalDate overlapStart = budget.getStartDate().isBefore(yearStart) ? yearStart : budget.getStartDate();
        LocalDate overlapEnd = budget.getEndDate().isAfter(yearEnd) ? yearEnd : budget.getEndDate();

        if (overlapStart.isAfter(overlapEnd)) {
            return BigDecimal.ZERO;
        }

        long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
        long overlapMonths = countMonthsOverlap(overlapStart, overlapEnd);

        // If a budget only covers a single month, treat it as a recurring budget for projection.
        // Budget tracking is month-scoped, but projections should reflect a full-year run rate.
        if (overlapMonths == 1) {
            long daysInYear = ChronoUnit.DAYS.between(yearStart, yearEnd) + 1;
            return switch (budget.getPeriodType()) {
                case MONTHLY -> budget.getAmount().multiply(BigDecimal.valueOf(12));
                case WEEKLY -> budget.getAmount().multiply(
                        BigDecimal.valueOf(daysInYear).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP));
                case QUARTERLY -> budget.getAmount().multiply(BigDecimal.valueOf(4));
                case YEARLY -> budget.getAmount();
            };
        }

        return switch (budget.getPeriodType()) {
            case MONTHLY -> budget.getAmount().multiply(BigDecimal.valueOf(overlapMonths));
            case WEEKLY -> budget.getAmount().multiply(
                    BigDecimal.valueOf(overlapDays).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP));
            case QUARTERLY -> budget.getAmount().multiply(
                    BigDecimal.valueOf(overlapMonths).divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP));
            case YEARLY -> budget.getAmount().multiply(
                    BigDecimal.valueOf(overlapDays).divide(BigDecimal.valueOf(365), 4, RoundingMode.HALF_UP));
        };
    }

    /**
     * Count the number of full or partial months the budget overlaps with the year.
     */
    private long countMonthsOverlap(final LocalDate overlapStart, final LocalDate overlapEnd) {
        YearMonth startMonth = YearMonth.from(overlapStart);
        YearMonth endMonth = YearMonth.from(overlapEnd);
        return startMonth.until(endMonth, java.time.temporal.ChronoUnit.MONTHS) + 1;
    }

    /**
     * Project subscription costs using the current month's total × 12.
     * Mirrors the housing projection approach — subscriptions are stored as per-month records,
     * so summing annual equivalents of all records would massively overcount.
     */
    BigDecimal projectSubscriptionCosts(final Long userId) {
        return subscriptionService.calculateCurrentMonthTotal(userId)
                .multiply(BigDecimal.valueOf(12));
    }

    /**
     * Project debt payments using minimum payment × 12 for active debts with balance > 0.
     */
    BigDecimal projectDebtPayments(final User user) {
        List<DebtDTO> debts = debtService.findAllByUserAsDto(user);
        return debts.stream()
                .filter(debt -> Boolean.TRUE.equals(debt.getActive()))
                .filter(debt -> debt.getCurrentBalance() != null && debt.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0)
                .filter(debt -> debt.getMinimumPayment() != null)
                .map(debt -> debt.getMinimumPayment().multiply(BigDecimal.valueOf(12)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Project holiday costs from holiday budgets for holidays overlapping the target year.
     */
    BigDecimal projectHolidayCosts(final Long userId, final LocalDate yearStart, final LocalDate yearEnd) {
        List<HolidayDTO> holidays = holidayService.findActiveByUserIdAsDto(userId);
        return holidays.stream()
                .filter(h -> h.getStartDate() != null && h.getBudget() != null)
                .filter(h -> !h.getStartDate().isAfter(yearEnd) &&
                        (h.getEndDate() == null || !h.getEndDate().isBefore(yearStart)))
                .map(HolidayDTO::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Build the combined category projection list from budget categories + synthetic categories.
     */
    private List<CategoryProjection> buildCategoryProjections(
            final Map<String, CategoryProjection> budgetCategories,
            final BigDecimal housingCosts,
            final BigDecimal debtPayments,
            final BigDecimal subscriptionCosts,
            final BigDecimal holidayCosts,
            final BigDecimal totalExpenses) {

        List<CategoryProjection> projections = new ArrayList<>();

        // Add budget-based categories with percentages
        for (CategoryProjection cat : budgetCategories.values()) {
            projections.add(CategoryProjection.builder()
                    .categoryId(cat.getCategoryId())
                    .categoryName(cat.getCategoryName())
                    .colorCode(cat.getColorCode())
                    .annualAmount(cat.getAnnualAmount())
                    .percentageOfTotal(calculatePercentage(cat.getAnnualAmount(), totalExpenses))
                    .periodType(cat.getPeriodType())
                    .synthetic(false)
                    .build());
        }

        // Add synthetic categories
        addSyntheticCategory(projections, FinancialThresholds.HOUSING_CATEGORY_ID, "Housing",
                FinancialThresholds.HOUSING_CATEGORY_COLOR, housingCosts, totalExpenses, "HOUSING");
        addSyntheticCategory(projections, FinancialThresholds.DEBT_CATEGORY_ID, "Debt Payments",
                FinancialThresholds.DEBT_CATEGORY_COLOR, debtPayments, totalExpenses, "DEBT");
        addSyntheticCategory(projections, FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID, "Subscriptions",
                FinancialThresholds.SUBSCRIPTIONS_CATEGORY_COLOR, subscriptionCosts, totalExpenses, "SUBSCRIPTIONS");
        addSyntheticCategory(projections, FinancialThresholds.HOLIDAYS_CATEGORY_ID, "Holidays",
                FinancialThresholds.HOLIDAYS_CATEGORY_COLOR, holidayCosts, totalExpenses, "HOLIDAYS");

        projections.sort(Comparator.comparing(CategoryProjection::getAnnualAmount).reversed());
        return projections;
    }

    private void addSyntheticCategory(
            final List<CategoryProjection> projections,
            final Long categoryId,
            final String name,
            final String color,
            final BigDecimal amount,
            final BigDecimal totalExpenses,
            final String periodType) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            projections.add(CategoryProjection.builder()
                    .categoryId(categoryId)
                    .categoryName(name)
                    .colorCode(color)
                    .annualAmount(amount)
                    .percentageOfTotal(calculatePercentage(amount, totalExpenses))
                    .periodType(periodType)
                    .synthetic(true)
                    .build());
        }
    }

    /**
     * Populate blended view fields: YTD actuals + remaining projected for the current year.
     */
    private void populateBlendedView(
            final AnnualProjectionDTO.AnnualProjectionDTOBuilder builder,
            final Long userId,
            final User user,
            final int year,
            final BigDecimal projectedIncome,
            final BigDecimal projectedExpenses) {

        int currentMonth = LocalDate.now().getMonthValue();
        int completedMonths = currentMonth - 1;
        int projectedMonths = 12 - completedMonths;

        // Sum actuals for completed months
        BigDecimal ytdActualIncome = BigDecimal.ZERO;
        BigDecimal ytdActualExpenses = BigDecimal.ZERO;

        for (int month = 1; month <= completedMonths; month++) {
            YearMonth ym = YearMonth.of(year, month);
            MonthlySummaryDTO summary = monthlySummaryService.generateMonthlySummary(userId, ym);
            ytdActualIncome = ytdActualIncome.add(summary.getTotalIncome());
            ytdActualExpenses = ytdActualExpenses.add(summary.getTotalExpenses());
        }

        // Pro-rate projected amounts for remaining months
        BigDecimal remainingProjectedIncome = projectedMonths > 0
                ? projectedIncome.multiply(BigDecimal.valueOf(projectedMonths))
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal remainingProjectedExpenses = projectedMonths > 0
                ? projectedExpenses.multiply(BigDecimal.valueOf(projectedMonths))
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal blendedIncome = ytdActualIncome.add(remainingProjectedIncome);
        BigDecimal blendedExpenses = ytdActualExpenses.add(remainingProjectedExpenses);
        BigDecimal blendedNetSavings = blendedIncome.subtract(blendedExpenses);
        BigDecimal blendedSavingsRate = calculateSavingsRate(blendedIncome, blendedNetSavings);

        builder.ytdActualIncome(ytdActualIncome)
                .ytdActualExpenses(ytdActualExpenses)
                .remainingProjectedIncome(remainingProjectedIncome)
                .remainingProjectedExpenses(remainingProjectedExpenses)
                .blendedAnnualIncome(blendedIncome)
                .blendedAnnualExpenses(blendedExpenses)
                .blendedNetSavings(blendedNetSavings)
                .blendedSavingsRate(blendedSavingsRate)
                .completedMonths(completedMonths)
                .projectedMonths(projectedMonths);
    }

    private BigDecimal calculateSavingsRate(final BigDecimal income, final BigDecimal netSavings) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netSavings.multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(final BigDecimal part, final BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP);
    }

    private boolean isSyntheticCategory(final String categoryName) {
        if (categoryName == null) {
            return false;
        }
        return FinancialThresholds.HOLIDAYS_CATEGORY_NAME.equalsIgnoreCase(categoryName)
                || FinancialThresholds.SUBSCRIPTIONS_CATEGORY_NAME.equalsIgnoreCase(categoryName);
    }

    private User getUserById(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
    }
}
