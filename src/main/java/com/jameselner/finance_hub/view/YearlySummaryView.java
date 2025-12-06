package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthlyDataPointDTO;
import com.jameselner.finance_hub.dto.YearComparisonDTO;
import com.jameselner.finance_hub.dto.YearlySummaryDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.YearlySummaryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "yearly-summary", layout = MainLayout.class)
@PageTitle("Yearly Summary | Finance Hub")
@PermitAll
public class YearlySummaryView extends VerticalLayout {

    private final YearlySummaryService yearlySummaryService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private ComboBox<Integer> yearSelector;
    private Integer selectedYear;

    private VerticalLayout summaryCardsLayout;
    private VerticalLayout monthlyChartSection;
    private VerticalLayout savingsRateSection;
    private VerticalLayout yearComparisonSection;
    private VerticalLayout topCategoriesSection;
    private VerticalLayout statisticsSection;

    private YearlySummaryDTO currentSummary;

    public YearlySummaryView(
            final YearlySummaryService yearlySummaryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.yearlySummaryService = yearlySummaryService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
        this.selectedYear = LocalDate.now().getYear();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createYearSelector()
        );

        // Placeholder layouts that will be populated
        summaryCardsLayout = new VerticalLayout();
        monthlyChartSection = new VerticalLayout();
        savingsRateSection = new VerticalLayout();
        yearComparisonSection = new VerticalLayout();
        topCategoriesSection = new VerticalLayout();
        statisticsSection = new VerticalLayout();

        add(
                summaryCardsLayout,
                monthlyChartSection,
                savingsRateSection,
                yearComparisonSection,
                topCategoriesSection,
                statisticsSection
        );

        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Annual Financial Summary");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private HorizontalLayout createYearSelector() {
        yearSelector = new ComboBox<>("Select Year");
        yearSelector.setItems(generateYearOptions());
        yearSelector.setValue(selectedYear);
        yearSelector.addValueChangeListener(event -> {
            selectedYear = event.getValue();
            refreshData();
        });

        Button exportButton = new Button("Export to CSV", new Icon(VaadinIcon.DOWNLOAD));
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(event -> exportToCSV());

        HorizontalLayout toolbar = new HorizontalLayout(yearSelector, exportButton);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private List<Integer> generateYearOptions() {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i < 10; i++) {
            years.add(currentYear - i);
        }
        return years;
    }

    private void refreshData() {
        User currentUser = getCurrentUser();
        currentSummary = yearlySummaryService.generateYearlySummary(
                currentUser.getUserId(), selectedYear);

        updateSummaryCards(currentSummary);
        updateMonthlyChart(currentSummary);
        updateSavingsRate(currentSummary);
        updateYearComparison(currentSummary);
        updateTopCategories(currentSummary);
        updateStatistics(currentSummary);
    }

    private void updateSummaryCards(final YearlySummaryDTO summary) {
        summaryCardsLayout.removeAll();

        H3 sectionTitle = new H3("Annual Overview");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        Div incomeCard = createMetricCard("Total Income", summary.getTotalIncome(),
                "var(--lumo-success-color)", VaadinIcon.ARROW_DOWN);
        Div expensesCard = createMetricCard("Total Expenses", summary.getTotalExpenses(),
                "var(--lumo-error-color)", VaadinIcon.ARROW_UP);
        Div savingsCard = createMetricCard("Net Savings", summary.getNetSavings(),
                summary.getNetSavings().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.PIGGY_BANK);
        Div savingsRateCard = createPercentageCard("Savings Rate", summary.getSavingsRate(),
                summary.getSavingsRate().compareTo(BigDecimal.valueOf(20)) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-warning-color)",
                VaadinIcon.CHART_LINE);

        cardsLayout.add(incomeCard, expensesCard, savingsCard, savingsRateCard);

        summaryCardsLayout.add(sectionTitle, cardsLayout);
    }

    private void updateMonthlyChart(final YearlySummaryDTO summary) {
        monthlyChartSection.removeAll();
        monthlyChartSection.setPadding(true);
        monthlyChartSection.setSpacing(true);
        monthlyChartSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Monthly Trends");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        monthlyChartSection.add(sectionTitle);

        if (summary.getMonthlyData() != null && !summary.getMonthlyData().isEmpty()) {
            // Create legend
            HorizontalLayout legend = new HorizontalLayout();
            legend.setSpacing(true);
            legend.add(createLegendItem("Income", "var(--lumo-success-color)"));
            legend.add(createLegendItem("Expenses", "var(--lumo-error-color)"));
            legend.add(createLegendItem("Savings", "var(--lumo-primary-color)"));
            monthlyChartSection.add(legend);

            // Find max value for scaling
            BigDecimal maxValue = summary.getMonthlyData().stream()
                    .map(m -> m.getIncome().max(m.getExpenses()))
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.valueOf(1000));

            // Ensure maxValue is never zero to avoid division by zero
            if (maxValue.compareTo(BigDecimal.ZERO) == 0) {
                maxValue = BigDecimal.valueOf(1000);
            }

            // Create chart
            for (MonthlyDataPointDTO dataPoint : summary.getMonthlyData()) {
                monthlyChartSection.add(createMonthBar(dataPoint, maxValue));
            }

            // Add averages
            HorizontalLayout averagesLayout = new HorizontalLayout();
            averagesLayout.setWidthFull();
            averagesLayout.setSpacing(true);
            averagesLayout.getStyle().set("margin-top", "1rem");

            Div avgIncomeCard = createMetricCard("Avg Monthly Income",
                    summary.getAverageMonthlyIncome(),
                    "var(--lumo-success-color)", VaadinIcon.COIN_PILES, true);
            Div avgExpensesCard = createMetricCard("Avg Monthly Expenses",
                    summary.getAverageMonthlyExpenses(),
                    "var(--lumo-error-color)", VaadinIcon.WALLET, true);
            Div avgSavingsCard = createMetricCard("Avg Monthly Savings",
                    summary.getAverageMonthlySavings(),
                    "var(--lumo-primary-color)", VaadinIcon.PIGGY_BANK, true);

            averagesLayout.add(avgIncomeCard, avgExpensesCard, avgSavingsCard);
            monthlyChartSection.add(averagesLayout);
        } else {
            Span noDataLabel = new Span("No data available for this year");
            noDataLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            monthlyChartSection.add(noDataLabel);
        }
    }

    private Div createLegendItem(final String label, final String color) {
        Div item = new Div();
        item.getStyle().set("display", "flex").set("align-items", "center");

        Span colorBox = new Span();
        colorBox.getStyle()
                .set("width", "16px")
                .set("height", "16px")
                .set("background-color", color)
                .set("border-radius", "2px")
                .set("margin-right", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        item.add(colorBox, labelSpan);
        return item;
    }

    private VerticalLayout createMonthBar(final MonthlyDataPointDTO dataPoint, final BigDecimal maxValue) {
        VerticalLayout monthLayout = new VerticalLayout();
        monthLayout.setSpacing(false);
        monthLayout.setPadding(false);
        monthLayout.getStyle().set("margin-bottom", "1rem");

        // Month label
        Span monthLabel = new Span(dataPoint.getMonth().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        monthLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("margin-bottom", "0.25rem");

        // Bar container
        HorizontalLayout barContainer = new HorizontalLayout();
        barContainer.setWidthFull();
        barContainer.setSpacing(false);
        barContainer.getStyle()
                .set("height", "40px")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("overflow", "hidden")
                .set("position", "relative");

        // Calculate widths
        double incomeWidth = dataPoint.getIncome().divide(maxValue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        double expensesWidth = dataPoint.getExpenses().divide(maxValue, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();

        // Income bar
        Div incomeBar = new Div();
        incomeBar.getStyle()
                .set("width", incomeWidth + "%")
                .set("height", "20px")
                .set("background-color", "var(--lumo-success-color)")
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0");

        // Expenses bar
        Div expensesBar = new Div();
        expensesBar.getStyle()
                .set("width", expensesWidth + "%")
                .set("height", "20px")
                .set("background-color", "var(--lumo-error-color)")
                .set("position", "absolute")
                .set("top", "20px")
                .set("left", "0");

        barContainer.add(incomeBar, expensesBar);

        // Values
        HorizontalLayout valuesLayout = new HorizontalLayout();
        valuesLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        valuesLayout.setWidthFull();

        Span incomeValue = new Span(String.format("£%.2f", dataPoint.getIncome()));
        incomeValue.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-success-text-color)");

        Span expensesValue = new Span(String.format("£%.2f", dataPoint.getExpenses()));
        expensesValue.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-error-text-color)");

        Span savingsValue = new Span(String.format("Saved: £%.2f", dataPoint.getSavings()));
        savingsValue.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", dataPoint.getSavings().compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");

        valuesLayout.add(incomeValue, savingsValue, expensesValue);

        monthLayout.add(monthLabel, barContainer, valuesLayout);

        return monthLayout;
    }

    private void updateSavingsRate(final YearlySummaryDTO summary) {
        savingsRateSection.removeAll();
        savingsRateSection.setPadding(true);
        savingsRateSection.setSpacing(true);
        savingsRateSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Best Performing Months");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout highlightsLayout = new HorizontalLayout();
        highlightsLayout.setWidthFull();
        highlightsLayout.setSpacing(true);

        Div highestIncomeCard = createHighlightCard(
                "Highest Income Month",
                summary.getHighestIncomeMonth(),
                summary.getHighestIncomeAmount(),
                VaadinIcon.TRENDING_UP,
                "var(--lumo-success-color)"
        );

        Div bestSavingsCard = createHighlightCard(
                "Best Savings Month",
                summary.getBestSavingsMonth(),
                summary.getBestSavingsAmount(),
                VaadinIcon.PIGGY_BANK,
                "var(--lumo-primary-color)"
        );

        Div highestExpenseCard = createHighlightCard(
                "Highest Expense Month",
                summary.getHighestExpenseMonth(),
                summary.getHighestExpenseAmount(),
                VaadinIcon.TRENDING_DOWN,
                "var(--lumo-error-color)"
        );

        highlightsLayout.add(highestIncomeCard, bestSavingsCard, highestExpenseCard);

        savingsRateSection.add(sectionTitle, highlightsLayout);
    }

    private void updateYearComparison(final YearlySummaryDTO summary) {
        yearComparisonSection.removeAll();
        yearComparisonSection.setPadding(true);
        yearComparisonSection.setSpacing(true);
        yearComparisonSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        if (summary.getYearComparison() != null) {
            YearComparisonDTO comparison = summary.getYearComparison();

            H3 sectionTitle = new H3(String.format("Year-over-Year Growth (%d vs %d)",
                    comparison.getCurrentYear(), comparison.getPreviousYear()));
            sectionTitle.getStyle().set("margin", "0 0 1rem 0");

            yearComparisonSection.add(sectionTitle);

            HorizontalLayout comparisonCards = new HorizontalLayout();
            comparisonCards.setWidthFull();
            comparisonCards.setSpacing(true);

            Div incomeChangeCard = createChangeCard("Income Growth",
                    comparison.getIncomeChange(),
                    comparison.getIncomeChangePercent());

            Div expenseChangeCard = createChangeCard("Expense Change",
                    comparison.getExpenseChange(),
                    comparison.getExpenseChangePercent());

            Div savingsChangeCard = createChangeCard("Savings Growth",
                    comparison.getSavingsChange(),
                    comparison.getSavingsChangePercent());

            comparisonCards.add(incomeChangeCard, expenseChangeCard, savingsChangeCard);

            yearComparisonSection.add(comparisonCards);

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
                        .set("font-weight", "500")
                        .set("font-size", "var(--lumo-font-size-l)");
                yearComparisonSection.add(savingsRateInfo);
            }
        }
    }

    private void updateTopCategories(final YearlySummaryDTO summary) {
        topCategoriesSection.removeAll();
        topCategoriesSection.setPadding(true);
        topCategoriesSection.setSpacing(true);
        topCategoriesSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Top Spending Categories");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        topCategoriesSection.add(sectionTitle);

        if (summary.getTopSpendingCategories() != null && !summary.getTopSpendingCategories().isEmpty()) {
            for (CategorySpendingDTO category : summary.getTopSpendingCategories()) {
                topCategoriesSection.add(createCategoryRow(category));
            }
        } else {
            Span noDataLabel = new Span("No spending data for this year");
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

        row.add(header, progressBarContainer, details);

        return row;
    }

    private void updateStatistics(final YearlySummaryDTO summary) {
        statisticsSection.removeAll();
        statisticsSection.setPadding(true);
        statisticsSection.setSpacing(true);
        statisticsSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Year-End Statistics");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout statsCards = new HorizontalLayout();
        statsCards.setWidthFull();
        statsCards.setSpacing(true);

        Div transactionCountCard = createStatCard("Total Transactions",
                String.valueOf(summary.getTotalTransactions()),
                VaadinIcon.LIST);

        Div largestTransactionCard = createMetricCard("Largest Transaction",
                summary.getLargestTransaction(),
                "var(--lumo-warning-color)", VaadinIcon.WARNING, true);

        Div housingCostsCard = createMetricCard("Total Housing Costs",
                summary.getTotalHousingCosts(),
                "var(--lumo-primary-color)", VaadinIcon.HOME, true);

        Div budgetUtilizationCard = createPercentageCard("Budget Utilization",
                summary.getBudgetUtilization(),
                summary.getBudgetUtilization().compareTo(BigDecimal.valueOf(100)) > 0 ?
                        "var(--lumo-error-color)" : "var(--lumo-success-color)",
                VaadinIcon.PROGRESSBAR, true);

        statsCards.add(transactionCountCard, largestTransactionCard, housingCostsCard, budgetUtilizationCard);

        statisticsSection.add(sectionTitle, statsCards);

        if (summary.getLargestTransactionCategory() != null && !"N/A".equals(summary.getLargestTransactionCategory())) {
            Span largestCategoryInfo = new Span("Category: " + summary.getLargestTransactionCategory());
            largestCategoryInfo.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            statisticsSection.add(largestCategoryInfo);
        }
    }

    private void exportToCSV() {
        if (currentSummary == null) {
            return;
        }

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Annual Financial Summary - ").append(selectedYear).append("\n\n");

        // Annual Totals
        csv.append("Annual Totals\n");
        csv.append("Total Income,").append(currentSummary.getTotalIncome()).append("\n");
        csv.append("Total Expenses,").append(currentSummary.getTotalExpenses()).append("\n");
        csv.append("Net Savings,").append(currentSummary.getNetSavings()).append("\n");
        csv.append("Savings Rate,").append(currentSummary.getSavingsRate()).append("%\n\n");

        // Monthly Data
        csv.append("Monthly Breakdown\n");
        csv.append("Month,Income,Expenses,Savings,Savings Rate\n");
        for (MonthlyDataPointDTO month : currentSummary.getMonthlyData()) {
            csv.append(month.getMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append(",")
                    .append(month.getIncome()).append(",")
                    .append(month.getExpenses()).append(",")
                    .append(month.getSavings()).append(",")
                    .append(month.getSavingsRate()).append("%\n");
        }

        csv.append("\n");

        // Top Categories
        csv.append("Top Spending Categories\n");
        csv.append("Category,Amount,Percentage,Transactions\n");
        for (CategorySpendingDTO category : currentSummary.getTopSpendingCategories()) {
            csv.append(category.getCategoryName()).append(",")
                    .append(category.getTotalSpent()).append(",")
                    .append(category.getPercentageOfTotal()).append("%,")
                    .append(category.getTransactionCount()).append("\n");
        }

        // Create download using Anchor with href
        String csvContent = csv.toString();
        String dataUrl = "data:text/csv;charset=utf-8," +
                java.net.URLEncoder.encode(csvContent, StandardCharsets.UTF_8);

        Anchor download = new Anchor();
        download.setHref(dataUrl);
        download.getElement().setAttribute("download", "yearly-summary-" + selectedYear + ".csv");
        download.getStyle().set("display", "none");
        add(download);
        download.getElement().callJsFunction("click");
        remove(download);
    }

    private Div createMetricCard(final String label, final BigDecimal value,
                                  final String color, final VaadinIcon icon) {
        return createMetricCard(label, value, color, icon, false);
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
        return createPercentageCard(label, value, color, icon, false);
    }

    private Div createPercentageCard(final String label, final BigDecimal value,
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

        Span valueSpan = new Span(String.format("%.1f%%", value != null ? value : BigDecimal.ZERO));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", compact ? "var(--lumo-font-size-xl)" : "var(--lumo-font-size-xxl)")
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

    private Div createHighlightCard(final String label, final String monthName,
                                     final BigDecimal amount, final VaadinIcon icon, final String color) {
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

        Span monthSpan = new Span(monthName);
        monthSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0.5rem 0");

        Span valueSpan = new Span(String.format("£%.2f", amount != null ? amount : BigDecimal.ZERO));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", color);

        card.add(cardIcon, labelSpan, monthSpan, valueSpan);

        return card;
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
