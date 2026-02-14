package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO.CategoryProjection;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.service.AnnualProjectionService;
import com.jameselner.finance_hub.service.BudgetService;
import com.jameselner.finance_hub.service.CurrentUserService;
import com.jameselner.finance_hub.util.CurrencyFormatter;
import com.jameselner.finance_hub.view.components.MetricCardFactory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "projections", layout = MainLayout.class)
@PageTitle("Projections | Finance Hub")
@PermitAll
public class ProjectionsView extends VerticalLayout {

    private final AnnualProjectionService annualProjectionService;
    private final BudgetService budgetService;
    private final CurrentUserService currentUserService;
    private final CurrencyFormatter currencyFormatter;
    private final MetricCardFactory metricCardFactory;

    private Integer selectedYear;
    private final Map<Long, BigDecimal> budgetOverrides = new HashMap<>();
    private BigDecimal incomeOverride;
    private BigDecimal housingOverride;
    private BigDecimal debtOverride;
    private BigDecimal subscriptionOverride;
    private BigDecimal holidayOverride;

    private AnnualProjectionDTO baseProjection;
    private AnnualProjectionDTO adjustedProjection;
    private List<BudgetDTO> currentBudgets;

    private final VerticalLayout summaryCardsLayout;
    private final VerticalLayout breakdownCardsLayout;
    private final VerticalLayout whatIfSection;
    private final VerticalLayout categoryProjectionsSection;

    public ProjectionsView(
            final AnnualProjectionService annualProjectionService,
            final BudgetService budgetService,
            final CurrentUserService currentUserService,
            final CurrencyFormatter currencyFormatter,
            final MetricCardFactory metricCardFactory
    ) {
        this.annualProjectionService = annualProjectionService;
        this.budgetService = budgetService;
        this.currentUserService = currentUserService;
        this.currencyFormatter = currencyFormatter;
        this.metricCardFactory = metricCardFactory;
        this.selectedYear = LocalDate.now().getYear();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeader(), createYearSelector());

        summaryCardsLayout = new VerticalLayout();
        breakdownCardsLayout = new VerticalLayout();
        whatIfSection = new VerticalLayout();
        categoryProjectionsSection = new VerticalLayout();

        add(summaryCardsLayout, breakdownCardsLayout, whatIfSection, categoryProjectionsSection);

        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Financial Projections");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private HorizontalLayout createYearSelector() {
        Select<Integer> yearSelector = new Select<>();
        yearSelector.setLabel("Select Year");
        yearSelector.setItems(generateYearOptions());
        yearSelector.setValue(selectedYear);
        yearSelector.addValueChangeListener(event -> {
            selectedYear = event.getValue();
            clearAllOverrides();
            refreshData();
        });

        HorizontalLayout toolbar = new HorizontalLayout(yearSelector);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        return toolbar;
    }

    private List<Integer> generateYearOptions() {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = -2; i <= 2; i++) {
            years.add(currentYear + i);
        }
        years.sort((a, b) -> b - a);
        return years;
    }

    private void refreshData() {
        Long userId = currentUserService.getCurrentUserId();
        LocalDate yearStart = LocalDate.of(selectedYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(selectedYear, 12, 31);

        baseProjection = annualProjectionService.generateProjection(userId, selectedYear);
        currentBudgets = budgetService.findByUserIdAndDateRange(userId, yearStart, yearEnd);

        recalculateAndRefreshUI();
    }

    private void recalculateAndRefreshUI() {
        Long userId = currentUserService.getCurrentUserId();

        if (hasAnyOverrides()) {
            adjustedProjection = annualProjectionService.generateProjectionWithOverrides(
                    userId, selectedYear, budgetOverrides,
                    incomeOverride, housingOverride, debtOverride,
                    subscriptionOverride, holidayOverride);
        } else {
            adjustedProjection = baseProjection;
        }

        updateSummaryCards(userId);
        updateBreakdownCards(userId);
        updateWhatIfSection(userId);
        updateCategoryProjections(userId);
    }

    private boolean hasAnyOverrides() {
        return !budgetOverrides.isEmpty()
                || incomeOverride != null
                || housingOverride != null
                || debtOverride != null
                || subscriptionOverride != null
                || holidayOverride != null;
    }

    private void clearAllOverrides() {
        budgetOverrides.clear();
        incomeOverride = null;
        housingOverride = null;
        debtOverride = null;
        subscriptionOverride = null;
        holidayOverride = null;
    }

    // ---- Summary Cards ----

    private void updateSummaryCards(final Long userId) {
        summaryCardsLayout.removeAll();

        H3 sectionTitle = new H3("Projected Annual Overview");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        boolean hasOverrides = hasAnyOverrides();

        Div incomeCard = createSummaryCard("Projected Income",
                adjustedProjection.getProjectedIncome(),
                hasOverrides ? baseProjection.getProjectedIncome() : null,
                "var(--lumo-success-color)", VaadinIcon.ARROW_DOWN, userId);

        Div expensesCard = createSummaryCard("Projected Expenses",
                adjustedProjection.getProjectedExpenses(),
                hasOverrides ? baseProjection.getProjectedExpenses() : null,
                "var(--lumo-error-color)", VaadinIcon.ARROW_UP, userId);

        BigDecimal netSavings = adjustedProjection.getProjectedNetSavings();
        Div savingsCard = createSummaryCard("Net Savings",
                netSavings,
                hasOverrides ? baseProjection.getProjectedNetSavings() : null,
                netSavings.compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)",
                VaadinIcon.PIGGY_BANK, userId);

        BigDecimal savingsRate = adjustedProjection.getProjectedSavingsRate();
        Div savingsRateCard = createSavingsRateCard("Savings Rate",
                savingsRate,
                hasOverrides ? baseProjection.getProjectedSavingsRate() : null,
                savingsRate.compareTo(BigDecimal.valueOf(20)) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-warning-color)",
                VaadinIcon.CHART_LINE);

        cardsLayout.add(incomeCard, expensesCard, savingsCard, savingsRateCard);
        summaryCardsLayout.add(sectionTitle, cardsLayout);
    }

    private Div createSummaryCard(final String label, final BigDecimal value,
                                   final BigDecimal baseValue, final String color,
                                   final VaadinIcon icon, final Long userId) {
        Div card = metricCardFactory.createMetricCard(
                label, currencyFormatter.format(value, userId), color, icon, false);

        if (baseValue != null) {
            BigDecimal delta = value.subtract(baseValue);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                String sign = delta.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
                Span deltaSpan = new Span(sign + currencyFormatter.format(delta, userId) + " from baseline");
                deltaSpan.getStyle()
                        .set("display", "block")
                        .set("font-size", "var(--lumo-font-size-xs)")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("margin-top", "0.25rem");
                card.add(deltaSpan);
            }
        }

        return card;
    }

    private Div createSavingsRateCard(final String label, final BigDecimal value,
                                       final BigDecimal baseValue, final String color,
                                       final VaadinIcon icon) {
        Div card = metricCardFactory.createPercentageCard(label, value, color, icon);

        if (baseValue != null) {
            BigDecimal delta = value.subtract(baseValue);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                String sign = delta.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
                Span deltaSpan = new Span(String.format("%s%.1f%% from baseline", sign, delta));
                deltaSpan.getStyle()
                        .set("display", "block")
                        .set("font-size", "var(--lumo-font-size-xs)")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("margin-top", "0.25rem");
                card.add(deltaSpan);
            }
        }

        return card;
    }

    // ---- Expense Breakdown Cards ----

    private void updateBreakdownCards(final Long userId) {
        breakdownCardsLayout.removeAll();

        H3 sectionTitle = new H3("Expense Breakdown");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        BigDecimal totalExpenses = adjustedProjection.getProjectedExpenses();

        cardsLayout.add(
                createBreakdownCard("Budgeted Categories", adjustedProjection.getBudgetedExpenses(),
                        totalExpenses, "var(--lumo-primary-color)", userId),
                createBreakdownCard("Housing", adjustedProjection.getHousingCosts(),
                        totalExpenses, "#4A90A4", userId),
                createBreakdownCard("Debt Payments", adjustedProjection.getDebtPayments(),
                        totalExpenses, "#E57373", userId),
                createBreakdownCard("Subscriptions", adjustedProjection.getSubscriptionCosts(),
                        totalExpenses, "#9575CD", userId),
                createBreakdownCard("Holidays", adjustedProjection.getHolidayCosts(),
                        totalExpenses, "#4DB6AC", userId)
        );

        breakdownCardsLayout.add(sectionTitle, cardsLayout);
    }

    private Div createBreakdownCard(final String label, final BigDecimal amount,
                                     final BigDecimal totalExpenses, final String color,
                                     final Long userId) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "0.75rem")
                .set("background-color", "var(--finance-card-bg)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        Span valueSpan = new Span(currencyFormatter.format(safeAmount, userId));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                ? safeAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Span percentSpan = new Span(String.format("%.1f%% of total", percentage));
        percentSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(labelSpan, valueSpan, percentSpan);
        return card;
    }

    // ---- What-If Adjustments ----

    private void updateWhatIfSection(final Long userId) {
        whatIfSection.removeAll();
        whatIfSection.setPadding(true);
        whatIfSection.setSpacing(true);
        whatIfSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Header with title and reset all button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 sectionTitle = new H3("What-If Adjustments");
        sectionTitle.getStyle().set("margin", "0");
        headerLayout.add(sectionTitle);

        if (hasAnyOverrides()) {
            Button resetAllButton = new Button("Reset All", event -> {
                clearAllOverrides();
                recalculateAndRefreshUI();
            });
            resetAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            resetAllButton.getStyle().set("color", "var(--lumo-error-text-color)");
            headerLayout.add(resetAllButton);
        }

        whatIfSection.add(headerLayout);

        // Income adjustment
        BigDecimal baseMonthlyIncome = baseProjection.getProjectedIncome()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        whatIfSection.add(createFixedAdjustmentRow(
                "Income", "var(--lumo-success-color)",
                baseMonthlyIncome, incomeOverride,
                newVal -> { incomeOverride = newVal; recalculateAndRefreshUI(); },
                userId));

        // Separator
        whatIfSection.add(createSectionDivider("Budget Categories"));

        // Budget category adjustments
        Map<Long, BudgetRow> categoryRows = buildCategoryRows();
        for (BudgetRow row : categoryRows.values()) {
            whatIfSection.add(createBudgetAdjustmentRow(row, userId));
        }

        // Fixed cost adjustments
        whatIfSection.add(createSectionDivider("Fixed & Recurring Costs"));

        BigDecimal baseMonthlyHousing = baseProjection.getHousingCosts()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        whatIfSection.add(createFixedAdjustmentRow(
                "Housing", "#4A90A4",
                baseMonthlyHousing, housingOverride,
                newVal -> { housingOverride = newVal; recalculateAndRefreshUI(); },
                userId));

        BigDecimal baseMonthlyDebt = baseProjection.getDebtPayments()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        whatIfSection.add(createFixedAdjustmentRow(
                "Debt Payments", "#E57373",
                baseMonthlyDebt, debtOverride,
                newVal -> { debtOverride = newVal; recalculateAndRefreshUI(); },
                userId));

        BigDecimal baseMonthlySubscriptions = baseProjection.getSubscriptionCosts()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        whatIfSection.add(createFixedAdjustmentRow(
                "Subscriptions", "#9575CD",
                baseMonthlySubscriptions, subscriptionOverride,
                newVal -> { subscriptionOverride = newVal; recalculateAndRefreshUI(); },
                userId));

        BigDecimal baseMonthlyHolidays = baseProjection.getHolidayCosts()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        whatIfSection.add(createFixedAdjustmentRow(
                "Holidays", "#4DB6AC",
                baseMonthlyHolidays, holidayOverride,
                newVal -> { holidayOverride = newVal; recalculateAndRefreshUI(); },
                userId));
    }

    private Div createSectionDivider(final String label) {
        Div divider = new Div();
        divider.getStyle()
                .set("margin", "0.75rem 0 0.25rem 0")
                .set("padding-bottom", "0.25rem")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em");

        divider.add(labelSpan);
        return divider;
    }

    private HorizontalLayout createFixedAdjustmentRow(
            final String name, final String color,
            final BigDecimal originalMonthly, final BigDecimal currentOverride,
            final java.util.function.Consumer<BigDecimal> onOverrideChanged,
            final Long userId) {

        boolean isModified = currentOverride != null;
        BigDecimal currentMonthly = isModified ? currentOverride : originalMonthly;

        HorizontalLayout rowLayout = new HorizontalLayout();
        rowLayout.setWidthFull();
        rowLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rowLayout.setSpacing(true);
        rowLayout.getStyle()
                .set("padding", "0.5rem 0.75rem")
                .set("border-radius", "var(--lumo-border-radius-s)");

        if (isModified) {
            rowLayout.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
        }

        // Color dot + name
        Div labelDiv = new Div();
        labelDiv.getStyle().set("min-width", "180px");

        Span colorDot = new Span();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", color)
                .set("display", "inline-block")
                .set("margin-right", "0.5rem");

        Span nameSpan = new Span(name);
        nameSpan.getStyle().set("font-weight", "500");

        labelDiv.add(colorDot, nameSpan);

        // Original amount label
        Span originalLabel = new Span(currencyFormatter.format(originalMonthly, userId) + "/mo");
        originalLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "100px");

        // NumberField for override
        NumberField overrideField = new NumberField();
        overrideField.setValue(currentMonthly.doubleValue());
        overrideField.setStepButtonsVisible(false);
        overrideField.setWidth("140px");
        overrideField.setPrefixComponent(new Span(getCurrencySymbol(userId)));
        overrideField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        overrideField.addValueChangeListener(event -> {
            Double newValue = event.getValue();
            if (newValue != null && newValue >= 0) {
                BigDecimal newMonthly = BigDecimal.valueOf(newValue).setScale(2, RoundingMode.HALF_UP);
                if (newMonthly.compareTo(originalMonthly) == 0) {
                    onOverrideChanged.accept(null);
                } else {
                    onOverrideChanged.accept(newMonthly);
                }
            }
        });

        // Annualized display
        BigDecimal annualized = currentMonthly.multiply(BigDecimal.valueOf(12));
        Span annualizedLabel = new Span(currencyFormatter.format(annualized, userId) + "/yr");
        annualizedLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "500")
                .set("min-width", "100px");

        rowLayout.add(labelDiv, originalLabel, overrideField, annualizedLabel);

        // Reset button (only when modified)
        if (isModified) {
            Button resetButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> {
                onOverrideChanged.accept(null);
            });
            resetButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR);
            resetButton.getElement().setAttribute("title", "Reset to original");
            rowLayout.add(resetButton);
        }

        return rowLayout;
    }

    private Map<Long, BudgetRow> buildCategoryRows() {
        Map<Long, BudgetRow> categoryRows = new LinkedHashMap<>();

        for (BudgetDTO budget : currentBudgets) {
            Long catId = budget.getCategoryId();
            BigDecimal monthlyAmount = normalizeToMonthly(budget.getAmount(), budget.getPeriodType());

            if (categoryRows.containsKey(catId)) {
                BudgetRow existing = categoryRows.get(catId);
                existing.originalMonthly = existing.originalMonthly.add(monthlyAmount);
            } else {
                BudgetRow row = new BudgetRow();
                row.categoryId = catId;
                row.categoryName = budget.getCategoryName();
                row.colorCode = budget.getCategoryColorCode();
                row.originalMonthly = monthlyAmount;
                categoryRows.put(catId, row);
            }
        }

        return categoryRows;
    }

    private BigDecimal normalizeToMonthly(final BigDecimal amount, final PeriodType periodType) {
        if (amount == null || periodType == null) {
            return BigDecimal.ZERO;
        }
        return switch (periodType) {
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case MONTHLY -> amount;
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    private HorizontalLayout createBudgetAdjustmentRow(final BudgetRow row, final Long userId) {
        boolean isModified = budgetOverrides.containsKey(row.categoryId);
        BigDecimal currentMonthly = isModified ? budgetOverrides.get(row.categoryId) : row.originalMonthly;

        HorizontalLayout rowLayout = new HorizontalLayout();
        rowLayout.setWidthFull();
        rowLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rowLayout.setSpacing(true);
        rowLayout.getStyle()
                .set("padding", "0.5rem 0.75rem")
                .set("border-radius", "var(--lumo-border-radius-s)");

        if (isModified) {
            rowLayout.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
        }

        // Color dot + category name
        Div labelDiv = new Div();
        labelDiv.getStyle().set("min-width", "180px");

        Span colorDot = new Span();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", row.colorCode != null ? row.colorCode : "#666")
                .set("display", "inline-block")
                .set("margin-right", "0.5rem");

        Span nameSpan = new Span(row.categoryName);
        nameSpan.getStyle().set("font-weight", "500");

        labelDiv.add(colorDot, nameSpan);

        // Original amount label
        Span originalLabel = new Span(currencyFormatter.format(row.originalMonthly, userId) + "/mo");
        originalLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "100px");

        // NumberField for override
        NumberField overrideField = new NumberField();
        overrideField.setValue(currentMonthly.doubleValue());
        overrideField.setStepButtonsVisible(false);
        overrideField.setWidth("140px");
        overrideField.setPrefixComponent(new Span(getCurrencySymbol(userId)));
        overrideField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        overrideField.addValueChangeListener(event -> {
            Double newValue = event.getValue();
            if (newValue != null && newValue >= 0) {
                BigDecimal newMonthly = BigDecimal.valueOf(newValue).setScale(2, RoundingMode.HALF_UP);
                if (newMonthly.compareTo(row.originalMonthly) == 0) {
                    budgetOverrides.remove(row.categoryId);
                } else {
                    budgetOverrides.put(row.categoryId, newMonthly);
                }
                recalculateAndRefreshUI();
            }
        });

        // Annualized display
        BigDecimal annualized = currentMonthly.multiply(BigDecimal.valueOf(12));
        Span annualizedLabel = new Span(currencyFormatter.format(annualized, userId) + "/yr");
        annualizedLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "500")
                .set("min-width", "100px");

        rowLayout.add(labelDiv, originalLabel, overrideField, annualizedLabel);

        // Reset button (only when modified)
        if (isModified) {
            Button resetButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> {
                budgetOverrides.remove(row.categoryId);
                recalculateAndRefreshUI();
            });
            resetButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR);
            resetButton.getElement().setAttribute("title", "Reset to original");
            rowLayout.add(resetButton);
        }

        return rowLayout;
    }

    private String getCurrencySymbol(final Long userId) {
        // Format zero to extract just the currency symbol
        String formatted = currencyFormatter.format(BigDecimal.ZERO, userId);
        // Strip digits, spaces, dots, commas to get the symbol
        return formatted.replaceAll("[\\d.,\\s]", "").trim();
    }

    // ---- Category Projections ----

    private void updateCategoryProjections(final Long userId) {
        categoryProjectionsSection.removeAll();
        categoryProjectionsSection.setPadding(true);
        categoryProjectionsSection.setSpacing(true);
        categoryProjectionsSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Projected Spending by Category");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");
        categoryProjectionsSection.add(sectionTitle);

        List<CategoryProjection> projections = adjustedProjection.getCategoryProjections();
        if (projections != null && !projections.isEmpty()) {
            for (CategoryProjection cat : projections) {
                categoryProjectionsSection.add(createProjectionCategoryRow(cat, userId));
            }
        } else {
            Span noData = new Span("No category projections available");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            categoryProjectionsSection.add(noData);
        }
    }

    private VerticalLayout createProjectionCategoryRow(final CategoryProjection category, final Long userId) {
        VerticalLayout row = new VerticalLayout();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle().set("margin-bottom", "0.75rem");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Div categoryLabel = new Div();
        Span colorDot = new Span();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", category.getColorCode() != null ? category.getColorCode() : "#666")
                .set("display", "inline-block")
                .set("margin-right", "0.5rem");

        Span name = new Span(category.getCategoryName());
        name.getStyle().set("font-weight", "500");

        categoryLabel.add(colorDot, name);

        Span amount = new Span(currencyFormatter.format(category.getAnnualAmount(), userId));
        amount.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        header.add(categoryLabel, amount);

        // Progress bar
        Div progressBarContainer = new Div();
        progressBarContainer.getStyle()
                .set("width", "100%")
                .set("height", "6px")
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("border-radius", "3px")
                .set("overflow", "hidden");

        BigDecimal pct = category.getPercentageOfTotal() != null ? category.getPercentageOfTotal() : BigDecimal.ZERO;
        Div progressBar = new Div();
        progressBar.getStyle()
                .set("height", "100%")
                .set("background-color", category.getColorCode() != null ?
                        category.getColorCode() : "var(--lumo-primary-color)")
                .set("width", pct + "%")
                .set("transition", "width 0.3s ease");

        progressBarContainer.add(progressBar);

        Span percentage = new Span(String.format("%.1f%% of projected expenses", pct));
        percentage.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        row.add(header, progressBarContainer, percentage);
        return row;
    }

    // ---- Helper class for budget rows ----

    private static class BudgetRow {
        Long categoryId;
        String categoryName;
        String colorCode;
        BigDecimal originalMonthly;
    }
}
