package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthComparisonDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.MonthlySummaryService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

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
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private ComboBox<YearMonth> monthSelector;
    private YearMonth selectedMonth;

    private VerticalLayout summaryCardsLayout;
    private VerticalLayout budgetPerformanceSection;
    private VerticalLayout topCategoriesSection;
    private VerticalLayout monthComparisonSection;
    private VerticalLayout statisticsSection;

    public MonthlySummaryView(
            final MonthlySummaryService monthlySummaryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.monthlySummaryService = monthlySummaryService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
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
        monthSelector = new ComboBox<>("Select Month");
        monthSelector.setItems(generateMonthOptions());
        monthSelector.setValue(selectedMonth);
        monthSelector.setItemLabelGenerator(month ->
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthSelector.addValueChangeListener(event -> {
            selectedMonth = event.getValue();
            refreshData();
        });

        HorizontalLayout toolbar = new HorizontalLayout(monthSelector);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private List<YearMonth> generateMonthOptions() {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 24; i++) {
            months.add(current.minusMonths(i));
        }
        return months;
    }

    private void refreshData() {
        User currentUser = getCurrentUser();
        MonthlySummaryDTO summary = monthlySummaryService.generateMonthlySummary(
                currentUser.getUserId(), selectedMonth);

        updateSummaryCards(summary);
        updateBudgetPerformance(summary);
        updateTopCategories(summary);
        updateMonthComparison(summary);
        updateStatistics(summary);
    }

    private void updateSummaryCards(final MonthlySummaryDTO summary) {
        summaryCardsLayout.removeAll();

        H3 sectionTitle = new H3("Financial Overview");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        Div incomeCard = createMetricCard("Total Income", summary.getTotalIncome(),
                "var(--lumo-success-color)", VaadinIcon.ARROW_DOWN, false);
        Div expensesCard = createMetricCard("Total Expenses", summary.getTotalExpenses(),
                "var(--lumo-error-color)", VaadinIcon.ARROW_UP, false);
        Div savingsCard = createMetricCard("Net Savings", summary.getNetSavings(),
                summary.getNetSavings().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.PIGGY_BANK, false);
        Div savingsRateCard = createPercentageCard("Savings Rate", summary.getSavingsRate(),
                summary.getSavingsRate().compareTo(BigDecimal.valueOf(20)) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-warning-color)",
                VaadinIcon.CHART_LINE);

        cardsLayout.add(incomeCard, expensesCard, savingsCard, savingsRateCard);

        summaryCardsLayout.add(sectionTitle, cardsLayout);
    }

    private void updateBudgetPerformance(final MonthlySummaryDTO summary) {
        budgetPerformanceSection.removeAll();
        budgetPerformanceSection.setPadding(true);
        budgetPerformanceSection.setSpacing(true);
        budgetPerformanceSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Budget Performance");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout budgetCards = new HorizontalLayout();
        budgetCards.setWidthFull();
        budgetCards.setSpacing(true);
        budgetCards.addClassName("mobile-stack");

        Div budgetedCard = createMetricCard("Total Budgeted", summary.getTotalBudgeted(),
                "var(--lumo-primary-color)", VaadinIcon.PIGGY_BANK, true);
        Div spentCard = createMetricCard("Total Spent", summary.getTotalSpent(),
                "var(--lumo-contrast-color)", VaadinIcon.CART, true);
        Div remainingCard = createMetricCard("Remaining", summary.getBudgetRemaining(),
                summary.getBudgetRemaining().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.WALLET, true);
        Div utilizationCard = createPercentageCard("Utilization", summary.getBudgetUtilization(),
                summary.getBudgetUtilization().compareTo(BigDecimal.valueOf(100)) > 0 ?
                        "var(--lumo-error-color)" : "var(--lumo-success-color)",
                VaadinIcon.PROGRESSBAR);

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

    private void updateTopCategories(final MonthlySummaryDTO summary) {
        topCategoriesSection.removeAll();
        topCategoriesSection.setPadding(true);
        topCategoriesSection.setSpacing(true);
        topCategoriesSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Spending Categories");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        topCategoriesSection.add(sectionTitle);

        if (summary.getTopSpendingCategories() != null && !summary.getTopSpendingCategories().isEmpty()) {
            for (CategorySpendingDTO category : summary.getTopSpendingCategories()) {
                VerticalLayout categoryRow = createCategoryRow(category);
                topCategoriesSection.add(categoryRow);
            }
        } else {
            Span noDataLabel = new Span("No spending data for this month");
            noDataLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            topCategoriesSection.add(noDataLabel);
        }
    }

    private VerticalLayout createCategoryRow(final CategorySpendingDTO category) {
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

        Span amount = new Span(String.format("£%.2f", category.getTotalSpent()));
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

        Div progressBar = new Div();
        progressBar.getStyle()
                .set("height", "100%")
                .set("background-color", category.getCategoryColor() != null ?
                        category.getCategoryColor() : "var(--lumo-primary-color)")
                .set("width", category.getPercentageOfTotal() + "%")
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
            Span budget = new Span("Budget: £" + String.format("%.2f", category.getBudgetAmount()));
            budget.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", category.getTotalSpent().compareTo(category.getBudgetAmount()) > 0 ?
                            "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            details.add(budget);
        }

        row.add(header, progressBarContainer, details);

        return row;
    }

    private void updateMonthComparison(final MonthlySummaryDTO summary) {
        monthComparisonSection.removeAll();
        monthComparisonSection.setPadding(true);
        monthComparisonSection.setSpacing(true);
        monthComparisonSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Month-over-Month Comparison");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        monthComparisonSection.add(sectionTitle);

        if (summary.getMonthComparison() != null) {
            MonthComparisonDTO comparison = summary.getMonthComparison();

            HorizontalLayout comparisonCards = new HorizontalLayout();
            comparisonCards.setWidthFull();
            comparisonCards.setSpacing(true);
            comparisonCards.addClassName("mobile-stack");

            Div incomeChangeCard = createChangeCard("Income Change",
                    comparison.getIncomeChange(),
                    comparison.getIncomeChangePercent());

            Div expenseChangeCard = createChangeCard("Expense Change",
                    comparison.getExpenseChange(),
                    comparison.getExpenseChangePercent());

            Div savingsChangeCard = createChangeCard("Savings Change",
                    comparison.getSavingsChange(),
                    comparison.getSavingsChangePercent());

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
        }
    }

    private void updateStatistics(final MonthlySummaryDTO summary) {
        statisticsSection.removeAll();
        statisticsSection.setPadding(true);
        statisticsSection.setSpacing(true);
        statisticsSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Summary Statistics");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout statsCards = new HorizontalLayout();
        statsCards.setWidthFull();
        statsCards.setSpacing(true);
        statsCards.addClassName("mobile-stack");

        Div transactionCountCard = createStatCard("Transactions",
                String.valueOf(summary.getTransactionCount()),
                VaadinIcon.LIST);

        Div avgTransactionCard = createMetricCard("Avg Transaction",
                summary.getAverageTransactionSize(),
                "var(--lumo-primary-color)", VaadinIcon.CALC, true);

        Div largestExpenseCard = createMetricCard("Largest Expense",
                summary.getLargestExpense(),
                "var(--lumo-warning-color)", VaadinIcon.WARNING, true);

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
                    "Housing costs: £%.2f/month (%.1f%% of income)",
                    summary.getHousingCosts(),
                    summary.getHousingToIncomeRatio()
            ));
            housingInfo.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", summary.getHousingToIncomeRatio().compareTo(BigDecimal.valueOf(30)) > 0 ?
                            "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            statisticsSection.add(housingInfo);
        }
    }

    private Div createMetricCard(final String label, final BigDecimal value,
                                  final String color, final VaadinIcon icon, final boolean compact) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", compact ? "0.75rem" : "1rem")
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(String.format("£%.2f", value != null ? value : BigDecimal.ZERO));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", compact ? "var(--lumo-font-size-xl)" : "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);

        return card;
    }

    private Div createPercentageCard(final String label, final BigDecimal value,
                                      final String color, final VaadinIcon icon) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "1rem")
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(String.format("%.1f%%", value != null ? value : BigDecimal.ZERO));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);

        return card;
    }

    private Div createStatCard(final String label, final String value, final VaadinIcon icon) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "0.75rem")
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid var(--lumo-primary-color)");

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor("var(--lumo-primary-color)");
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);

        return card;
    }

    private Div createChangeCard(final String label, final BigDecimal change, final BigDecimal changePercent) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "1rem")
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)");

        boolean isPositive = change.compareTo(BigDecimal.ZERO) >= 0;
        String color = isPositive ? "var(--lumo-success-color)" : "var(--lumo-error-color)";
        Icon arrow = new Icon(isPositive ? VaadinIcon.ARROW_UP : VaadinIcon.ARROW_DOWN);
        arrow.setColor(color);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "0.5rem");

        HorizontalLayout valueLayout = new HorizontalLayout(arrow);
        valueLayout.setSpacing(true);
        valueLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span changeValue = new Span(String.format("£%.2f", change.abs()));
        changeValue.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", color);

        valueLayout.add(changeValue);

        Span percentValue = new Span(String.format("(%.1f%%)", changePercent.abs()));
        percentValue.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", color);

        card.add(labelSpan, valueLayout, percentValue);

        return card;
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
