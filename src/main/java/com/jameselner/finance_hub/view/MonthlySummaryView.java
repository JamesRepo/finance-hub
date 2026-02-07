package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthComparisonDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
import com.jameselner.finance_hub.service.CurrentUserService;
import com.jameselner.finance_hub.service.MonthlySummaryService;
import com.jameselner.finance_hub.util.CurrencyFormatter;
import com.jameselner.finance_hub.util.FinancialThresholds;
import com.jameselner.finance_hub.view.components.MetricCardFactory;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "monthly-summary", layout = MainLayout.class)
@PageTitle("Monthly Summary | Finance Hub")
@PermitAll
public class MonthlySummaryView extends VerticalLayout {

    private final MonthlySummaryService monthlySummaryService;
    private final CurrentUserService currentUserService;
    private final CurrencyFormatter currencyFormatter;
    private final MetricCardFactory metricCardFactory;

    private YearMonth selectedMonth;

    private final VerticalLayout summaryCardsLayout;
    private final VerticalLayout budgetPerformanceSection;
    private final VerticalLayout topCategoriesSection;
    private final VerticalLayout monthComparisonSection;
    private final VerticalLayout statisticsSection;

    public MonthlySummaryView(
            final MonthlySummaryService monthlySummaryService,
            final CurrentUserService currentUserService,
            final CurrencyFormatter currencyFormatter,
            final MetricCardFactory metricCardFactory
    ) {
        this.monthlySummaryService = monthlySummaryService;
        this.currentUserService = currentUserService;
        this.currencyFormatter = currencyFormatter;
        this.metricCardFactory = metricCardFactory;
        this.selectedMonth = YearMonth.now();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createMonthSelector()
        );

        // Placeholder layouts that will be populated
        summaryCardsLayout = new VerticalLayout();
        budgetPerformanceSection = new VerticalLayout();
        topCategoriesSection = new VerticalLayout();
        monthComparisonSection = new VerticalLayout();
        statisticsSection = new VerticalLayout();

        add(
                summaryCardsLayout,
                budgetPerformanceSection,
                topCategoriesSection,
                monthComparisonSection,
                statisticsSection
        );

        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Monthly Financial Summary");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private HorizontalLayout createMonthSelector() {
        Select<YearMonth> monthSelector = new Select<>();
        monthSelector.setLabel("Select Month");
        monthSelector.setItems(generateMonthOptions());
        monthSelector.setValue(selectedMonth);
        monthSelector.setItemLabelGenerator(month ->
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthSelector.addValueChangeListener(event -> {
            selectedMonth = event.getValue();
            refreshData();
        });

        HorizontalLayout toolbar = new HorizontalLayout(monthSelector);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private List<YearMonth> generateMonthOptions() {
        Long userId = currentUserService.getCurrentUserId();
        YearMonth earliest = monthlySummaryService.getEarliestAvailableMonth(userId);
        YearMonth current = YearMonth.now();

        List<YearMonth> months = new ArrayList<>();
        YearMonth month = current;
        while (!month.isBefore(earliest)) {
            months.add(month);
            month = month.minusMonths(1);
        }
        return months;
    }

    private void refreshData() {
        Long userId = currentUserService.getCurrentUserId();

        MonthlySummaryDTO summary = monthlySummaryService.generateMonthlySummary(
                userId, selectedMonth);

        updateSummaryCards(summary, userId);
        updateBudgetPerformance(summary, userId);
        updateTopCategories(summary, userId);
        updateMonthComparison(summary, userId);
        updateStatistics(summary, userId);
    }

    private void applyCardSectionStyle(final VerticalLayout section) {
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");
    }

    private void updateSummaryCards(final MonthlySummaryDTO summary, final Long userId) {
        summaryCardsLayout.removeAll();

        H3 sectionTitle = new H3("Financial Overview");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        Div incomeCard = metricCardFactory.createMetricCard(
                "Total Income",
                currencyFormatter.format(summary.getTotalIncome(), userId),
                "var(--lumo-success-color)",
                VaadinIcon.ARROW_DOWN,
                false
        );

        Div expensesCard = metricCardFactory.createMetricCard(
                "Total Expenses",
                currencyFormatter.format(summary.getTotalExpenses(), userId),
                "var(--lumo-error-color)",
                VaadinIcon.ARROW_UP,
                false
        );

        Div savingsCard = metricCardFactory.createMetricCard(
                "Net Savings",
                currencyFormatter.format(summary.getNetSavings(), userId),
                summary.getNetSavings().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.PIGGY_BANK,
                false
        );

        Div savingsRateCard = metricCardFactory.createPercentageCard(
                "Savings Rate",
                summary.getSavingsRate(),
                summary.getSavingsRate().compareTo(FinancialThresholds.GOOD_SAVINGS_RATE_PERCENT) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-warning-color)",
                VaadinIcon.CHART_LINE
        );

        cardsLayout.add(incomeCard, expensesCard, savingsCard, savingsRateCard);

        summaryCardsLayout.add(sectionTitle, cardsLayout);
    }

    private void updateBudgetPerformance(final MonthlySummaryDTO summary, final Long userId) {
        budgetPerformanceSection.removeAll();
        applyCardSectionStyle(budgetPerformanceSection);

        H3 sectionTitle = new H3("Budget Performance");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout budgetCards = new HorizontalLayout();
        budgetCards.setWidthFull();
        budgetCards.setSpacing(true);
        budgetCards.addClassName("mobile-stack");

        Div budgetedCard = metricCardFactory.createMetricCard(
                "Total Budgeted",
                currencyFormatter.format(summary.getTotalBudgeted(), userId),
                "var(--lumo-primary-color)",
                VaadinIcon.PIGGY_BANK,
                true
        );

        Div spentCard = metricCardFactory.createMetricCard(
                "Total Spent",
                currencyFormatter.format(summary.getTotalSpent(), userId),
                "var(--lumo-contrast-color)",
                VaadinIcon.CART,
                true
        );

        Div remainingCard = metricCardFactory.createMetricCard(
                "Remaining",
                currencyFormatter.format(summary.getBudgetRemaining(), userId),
                summary.getBudgetRemaining().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.WALLET,
                true
        );

        Div utilizationCard = metricCardFactory.createPercentageCard(
                "Utilization",
                summary.getBudgetUtilization(),
                summary.getBudgetUtilization().compareTo(FinancialThresholds.BUDGET_EXCEEDED_PERCENT) > 0 ?
                        "var(--lumo-error-color)" : "var(--lumo-success-color)",
                VaadinIcon.PROGRESSBAR
        );

        budgetCards.add(budgetedCard, spentCard, remainingCard, utilizationCard);

        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setSpacing(true);

        Span onTrackBadge = new Span(summary.getBudgetsOnTrackCount() + " On Track");
        onTrackBadge.getElement().getThemeList().add("badge");
        onTrackBadge.getElement().getThemeList().add("success");

        Span overBudgetBadge = new Span(summary.getBudgetsOverCount() + " Over Budget");
        overBudgetBadge.getElement().getThemeList().add("badge");
        overBudgetBadge.getElement().getThemeList().add("error");

        statusLayout.add(onTrackBadge, overBudgetBadge);

        budgetPerformanceSection.add(sectionTitle, budgetCards, statusLayout);
    }

    private void updateTopCategories(final MonthlySummaryDTO summary, final Long userId) {
        topCategoriesSection.removeAll();
        applyCardSectionStyle(topCategoriesSection);

        H3 sectionTitle = new H3("Spending Breakdown");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");
        topCategoriesSection.add(sectionTitle);

        // Variable spending categories (transaction-based)
        BigDecimal variableMax = findMaxPercentage(summary.getVariableSpendingCategories());

        H4 variableTitle = new H4("Spending Categories");
        variableTitle.getStyle().set("margin", "0.5rem 0");
        topCategoriesSection.add(variableTitle);

        if (summary.getVariableSpendingCategories() != null && !summary.getVariableSpendingCategories().isEmpty()) {
            for (CategorySpendingDTO category : summary.getVariableSpendingCategories()) {
                topCategoriesSection.add(createCategoryRow(category, userId, variableMax));
            }
        } else {
            Span noDataLabel = new Span("No spending data for this month");
            noDataLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            topCategoriesSection.add(noDataLabel);
        }

        // Fixed & recurring costs
        BigDecimal fixedMax = findMaxPercentage(summary.getFixedCostCategories());

        H4 fixedTitle = new H4("Fixed & Recurring Costs");
        fixedTitle.getStyle().set("margin", "1rem 0 0.5rem 0");
        topCategoriesSection.add(fixedTitle);

        if (summary.getFixedCostCategories() != null && !summary.getFixedCostCategories().isEmpty()) {
            for (CategorySpendingDTO category : summary.getFixedCostCategories()) {
                topCategoriesSection.add(createCategoryRow(category, userId, fixedMax));
            }
        } else {
            Span noFixedLabel = new Span("No fixed costs for this month");
            noFixedLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            topCategoriesSection.add(noFixedLabel);
        }
    }

    private BigDecimal findMaxPercentage(final List<CategorySpendingDTO> categories) {
        BigDecimal max = BigDecimal.ONE;
        if (categories != null) {
            for (CategorySpendingDTO c : categories) {
                if (c.getPercentageOfTotal() != null && c.getPercentageOfTotal().compareTo(max) > 0) {
                    max = c.getPercentageOfTotal();
                }
            }
        }
        return max;
    }

    private VerticalLayout createCategoryRow(
            final CategorySpendingDTO category, final Long userId, final BigDecimal maxPercentage) {
        VerticalLayout row = new VerticalLayout();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle().set("margin-bottom", "1rem");

        // Header with category name and amount
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Div categoryLabel = new Div();
        Span colorDot = new Span();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", category.getCategoryColor() != null ?
                        category.getCategoryColor() : "#666")
                .set("display", "inline-block")
                .set("margin-right", "0.5rem");

        Span name = new Span(category.getCategoryName());
        name.getStyle().set("font-weight", "500");

        categoryLabel.add(colorDot, name);

        Span amount = new Span(currencyFormatter.format(category.getTotalSpent(), userId));
        amount.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        header.add(categoryLabel, amount);

        // Progress bar
        Div progressBarContainer = new Div();
        progressBarContainer.getStyle()
                .set("width", "100%")
                .set("height", "8px")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("overflow", "hidden");

        // Scale bar width relative to the largest category so the biggest bar fills ~100%
        BigDecimal relativeWidth = maxPercentage.compareTo(BigDecimal.ZERO) > 0
                ? category.getPercentageOfTotal()
                        .multiply(new BigDecimal("100"))
                        .divide(maxPercentage, 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Div progressBar = new Div();
        progressBar.getStyle()
                .set("height", "100%")
                .set("background-color", category.getCategoryColor() != null ?
                        category.getCategoryColor() : "var(--lumo-primary-color)")
                .set("width", relativeWidth + "%")
                .set("transition", "width 0.3s ease");

        progressBarContainer.add(progressBar);

        // Details
        HorizontalLayout details = new HorizontalLayout();
        details.setSpacing(true);

        Span percentage = new Span(String.format("%.1f%% of total", category.getPercentageOfTotal()));
        percentage.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span transactions = new Span(category.getTransactionCount() + " transactions");
        transactions.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        details.add(percentage, transactions);

        if (category.getBudgetAmount() != null) {
            Span budget = new Span("Budget: " + currencyFormatter.format(category.getBudgetAmount(), userId));
            budget.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", category.getTotalSpent().compareTo(category.getBudgetAmount()) > 0 ?
                            "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            details.add(budget);
        }

        row.add(header, progressBarContainer, details);

        return row;
    }

    private void updateMonthComparison(final MonthlySummaryDTO summary, final Long userId) {
        monthComparisonSection.removeAll();
        applyCardSectionStyle(monthComparisonSection);

        H3 sectionTitle = new H3("Month-over-Month Comparison");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        monthComparisonSection.add(sectionTitle);

        if (summary.getMonthComparison() != null) {
            MonthComparisonDTO comparison = summary.getMonthComparison();

            HorizontalLayout comparisonCards = new HorizontalLayout();
            comparisonCards.setWidthFull();
            comparisonCards.setSpacing(true);
            comparisonCards.addClassName("mobile-stack");

            Div incomeChangeCard = metricCardFactory.createChangeCard(
                    "Income Change",
                    comparison.getIncomeChange(),
                    comparison.getIncomeChangePercent(),
                    currencyFormatter.format(comparison.getIncomeChange().abs(), userId)
            );

            Div expenseChangeCard = metricCardFactory.createChangeCard(
                    "Expense Change",
                    comparison.getExpenseChange(),
                    comparison.getExpenseChangePercent(),
                    currencyFormatter.format(comparison.getExpenseChange().abs(), userId)
            );

            Div savingsChangeCard = metricCardFactory.createChangeCard(
                    "Savings Change",
                    comparison.getSavingsChange(),
                    comparison.getSavingsChangePercent(),
                    currencyFormatter.format(comparison.getSavingsChange().abs(), userId)
            );

            comparisonCards.add(incomeChangeCard, expenseChangeCard, savingsChangeCard);

            monthComparisonSection.add(comparisonCards);

            // Savings rate change
            if (comparison.getSavingsRateChange() != null) {
                Span savingsRateInfo = new Span(String.format(
                        "Savings rate %s by %.1f percentage points",
                        comparison.getSavingsRateChange().compareTo(BigDecimal.ZERO) >= 0 ?
                                "increased" : "decreased",
                        comparison.getSavingsRateChange().abs()
                ));
                savingsRateInfo.getStyle()
                        .set("color", comparison.getSavingsRateChange().compareTo(BigDecimal.ZERO) >= 0 ?
                                "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)")
                        .set("font-weight", "500");
                monthComparisonSection.add(savingsRateInfo);
            }
        } else {
            Span noDataLabel = new Span("No comparison data available");
            noDataLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            monthComparisonSection.add(noDataLabel);
        }
    }

    private void updateStatistics(final MonthlySummaryDTO summary, final Long userId) {
        statisticsSection.removeAll();
        applyCardSectionStyle(statisticsSection);

        H3 sectionTitle = new H3("Summary Statistics");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout statsCards = new HorizontalLayout();
        statsCards.setWidthFull();
        statsCards.setSpacing(true);
        statsCards.addClassName("mobile-stack");

        Div transactionCountCard = metricCardFactory.createStatCard(
                "Transactions",
                String.valueOf(summary.getTransactionCount()),
                VaadinIcon.LIST
        );

        Div avgTransactionCard = metricCardFactory.createMetricCard(
                "Avg Transaction",
                currencyFormatter.format(summary.getAverageTransactionSize(), userId),
                "var(--lumo-primary-color)",
                VaadinIcon.CALC,
                true
        );

        Div largestExpenseCard = metricCardFactory.createMetricCard(
                "Largest Expense",
                currencyFormatter.format(summary.getLargestExpense(), userId),
                "var(--lumo-warning-color)",
                VaadinIcon.WARNING,
                true
        );

        statsCards.add(transactionCountCard, avgTransactionCard, largestExpenseCard);

        statisticsSection.add(sectionTitle, statsCards);

        if (summary.getLargestExpenseCategory() != null && !"N/A".equals(summary.getLargestExpenseCategory())) {
            Span largestCategoryInfo = new Span("Category: " + summary.getLargestExpenseCategory());
            largestCategoryInfo.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            statisticsSection.add(largestCategoryInfo);
        }

        // Housing info
        if (summary.getHousingCosts() != null) {
            Span housingInfo = new Span(String.format(
                    "Housing costs: %s/month (%.1f%% of income)",
                    currencyFormatter.format(summary.getHousingCosts(), userId),
                    summary.getHousingToIncomeRatio()
            ));
            housingInfo.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", summary.getHousingToIncomeRatio().compareTo(FinancialThresholds.WARNING_HOUSING_RATIO_PERCENT) > 0 ?
                            "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            statisticsSection.add(housingInfo);
        }
    }
}
