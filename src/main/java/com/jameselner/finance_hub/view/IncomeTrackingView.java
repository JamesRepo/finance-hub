package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.IncomeDeductionDTO;
import com.jameselner.finance_hub.dto.IncomeForecastDTO;
import com.jameselner.finance_hub.dto.IncomeReportDTO;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.dto.IncomeVsExpenseDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.IncomeDeductionService;
import com.jameselner.finance_hub.service.IncomeForecastService;
import com.jameselner.finance_hub.service.IncomeReportService;
import com.jameselner.finance_hub.service.IncomeSourceService;
import com.jameselner.finance_hub.view.components.IncomeSourceForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "income", layout = MainLayout.class)
@PageTitle("Income Tracking | Finance Hub")
@PermitAll
public class IncomeTrackingView extends VerticalLayout {

    private final IncomeSourceService incomeSourceService;
    private final IncomeDeductionService incomeDeductionService;
    private final IncomeForecastService incomeForecastService;
    private final IncomeReportService incomeReportService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private Span totalIncomeSpan;
    private Span recurringIncomeSpan;
    private Span postDeductionIncomeSpan;
    private Span forecastedMonthlySpan;

    private ComboBox<String> yearFilterCombo;
    private Integer selectedYear = null; // null means "All Time"

    private VerticalLayout contentLayout;
    private Grid<IncomeSourceDTO> incomeSourceGrid;

    private IncomeSourceForm incomeSourceForm;
    private Dialog formDialog;

    private Tabs navigationTabs;
    private Tab sourcesTab;
    private Tab analysisTab;

    public IncomeTrackingView(
            final IncomeSourceService incomeSourceService,
            final IncomeDeductionService incomeDeductionService,
            final IncomeForecastService incomeForecastService,
            final IncomeReportService incomeReportService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository,
            final CategoryRepository categoryRepository
    ) {
        this.incomeSourceService = incomeSourceService;
        this.incomeDeductionService = incomeDeductionService;
        this.incomeForecastService = incomeForecastService;
        this.incomeReportService = incomeReportService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;

        addClassName("income-tracking-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createSummaryCards(),
                createNavigationTabs(),
                createContentLayout()
        );

        setupDialogs();
        showSourcesView();
        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Income Tracking");
        title.getStyle().set("margin", "0");

        // Year filter
        yearFilterCombo = new ComboBox<>();
        yearFilterCombo.setPlaceholder("Filter by year");
        yearFilterCombo.setWidth("150px");
        yearFilterCombo.setClearButtonVisible(false);

        // Populate years - current year and a few previous years, plus "All Time"
        int currentYear = LocalDate.now().getYear();
        List<String> yearOptions = new java.util.ArrayList<>();
        yearOptions.add("All Time");
        for (int year = currentYear; year >= currentYear - 5; year--) {
            yearOptions.add(String.valueOf(year));
        }
        yearFilterCombo.setItems(yearOptions);
        yearFilterCombo.setValue(String.valueOf(currentYear)); // Default to current year
        selectedYear = currentYear;

        yearFilterCombo.addValueChangeListener(event -> {
            String value = event.getValue();
            if ("All Time".equals(value)) {
                selectedYear = null;
            } else {
                selectedYear = Integer.parseInt(value);
            }
            refreshData();
        });

        HorizontalLayout header = new HorizontalLayout(title, yearFilterCombo);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        // Post-deduction card is first and prominent
        VerticalLayout postDeductionCard = createSummaryCard("Take-Home Income", "£0.00", VaadinIcon.WALLET, "post-deduction-income");
        postDeductionCard.getStyle()
                .set("background-color", "var(--lumo-success-color-10pct)")
                .set("border", "2px solid var(--lumo-success-color)")
                .set("flex", "1.5");

        cardsLayout.add(
                postDeductionCard,
                createSummaryCard("Gross Income", "£0.00", VaadinIcon.MONEY, "total-income"),
                createSummaryCard("Recurring Income", "£0.00", VaadinIcon.REFRESH, "recurring-income"),
                createSummaryCard("Forecasted Monthly", "£0.00", VaadinIcon.CALENDAR, "forecasted-monthly")
        );

        return cardsLayout;
    }

    private VerticalLayout createSummaryCard(
            final String label,
            final String initialValue,
            final VaadinIcon icon,
            final String cardType
    ) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("summary-card");
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("flex", "1");

        Icon cardIcon = icon.create();
        cardIcon.setSize("24px");
        cardIcon.getStyle().set("color", "var(--lumo-primary-color)");

        Span valueSpan = new Span(initialValue);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold");

        // Make post-deduction value extra prominent
        if ("post-deduction-income".equals(cardType)) {
            cardIcon.getStyle().set("color", "var(--lumo-success-color)");
            valueSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxxl)")
                    .set("color", "var(--lumo-success-color)");
        }

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(cardIcon, valueSpan, labelSpan);

        switch (cardType) {
            case "total-income":
                totalIncomeSpan = valueSpan;
                break;
            case "recurring-income":
                recurringIncomeSpan = valueSpan;
                break;
            case "post-deduction-income":
                postDeductionIncomeSpan = valueSpan;
                break;
            case "forecasted-monthly":
                forecastedMonthlySpan = valueSpan;
                break;
        }

        return card;
    }

    private Tabs createNavigationTabs() {
        sourcesTab = new Tab(new Icon(VaadinIcon.LIST), new Span("Income Sources"));
        analysisTab = new Tab(new Icon(VaadinIcon.CHART), new Span("Analysis"));

        navigationTabs = new Tabs(sourcesTab, analysisTab);
        navigationTabs.setWidthFull();

        navigationTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab == sourcesTab) {
                showSourcesView();
            } else if (selectedTab == analysisTab) {
                showAnalysisView();
            }
        });

        return navigationTabs;
    }

    private VerticalLayout createContentLayout() {
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);

        return contentLayout;
    }

    private void showSourcesView() {
        contentLayout.removeAll();

        Button addButton = new Button("Add Income Source", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openIncomeSourceForm(null));

        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        incomeSourceGrid = createIncomeSourceGrid();

        contentLayout.add(toolbar, incomeSourceGrid);
        refreshIncomeSourceGrid();
    }

    private Grid<IncomeSourceDTO> createIncomeSourceGrid() {
        Grid<IncomeSourceDTO> grid = new Grid<>(IncomeSourceDTO.class, false);
        grid.setSizeFull();

        grid.addColumn(dto -> dto.getCategoryName() != null ? dto.getCategoryName() : "Uncategorised")
                .setHeader("Category")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> dto.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Date")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getGrossAmount() != null ? dto.getGrossAmount() : dto.getAmount()))
                .setHeader("Gross")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getNetAmount() != null ? dto.getNetAmount() : dto.getAmount()))
                .setHeader("Net")
                .setAutoWidth(true);

        grid.addColumn(dto -> dto.getIsRecurring() ? dto.getRecurrenceFrequency().getDisplayName() : "One-time")
                .setHeader("Frequency")
                .setAutoWidth(true);

        grid.addColumn(dto -> {
            if (dto.getNextExpectedDate() != null) {
                return dto.getNextExpectedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            }
            return "-";
        })
                .setHeader("Next Expected")
                .setAutoWidth(true);

        grid.addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private Span createStatusBadge(final IncomeSourceDTO dto) {
        Span badge = new Span(dto.getIsActive() ? "Active" : "Inactive");
        badge.getElement().getThemeList().add("badge");

        if (dto.getIsActive()) {
            badge.getElement().getThemeList().add("success");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }

        return badge;
    }

    private HorizontalLayout createActionButtons(final IncomeSourceDTO dto) {
        Button copyButton = new Button(new Icon(VaadinIcon.COPY));
        copyButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        copyButton.getElement().setAttribute("title", "Copy to next month");
        copyButton.addClickListener(event -> copyToNextMonth(dto));

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        editButton.getElement().setAttribute("title", "Edit");
        editButton.addClickListener(event -> openIncomeSourceForm(dto));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("title", "Delete");
        deleteButton.addClickListener(event -> deleteIncomeSource(dto));

        HorizontalLayout actions = new HorizontalLayout(copyButton, editButton, deleteButton);
        actions.setSpacing(true);

        return actions;
    }

    private void copyToNextMonth(final IncomeSourceDTO sourceDto) {
        // Create a copy with date set to next month
        IncomeSourceDTO copy = new IncomeSourceDTO();
        copy.setCategoryId(sourceDto.getCategoryId());
        copy.setCategoryName(sourceDto.getCategoryName());
        copy.setGrossAmount(sourceDto.getGrossAmount());
        copy.setAmount(sourceDto.getAmount());
        copy.setIsRecurring(sourceDto.getIsRecurring());
        copy.setRecurrenceFrequency(sourceDto.getRecurrenceFrequency());
        copy.setIsActive(sourceDto.getIsActive());
        copy.setAutoCreateTransaction(sourceDto.getAutoCreateTransaction());
        copy.setDescription(sourceDto.getDescription());

        // Set date to next month (same day)
        if (sourceDto.getStartDate() != null) {
            copy.setStartDate(sourceDto.getStartDate().plusMonths(1));
        } else {
            copy.setStartDate(LocalDate.now().plusMonths(1));
        }

        // Load deductions from source and prepare them for copying
        List<IncomeDeductionDTO> sourceDeductions = incomeDeductionService.findByIncomeSourceAsDto(sourceDto.getIncomeSourceId());
        copy.setDeductions(sourceDeductions.stream()
                .map(d -> {
                    IncomeDeductionDTO deductionCopy = new IncomeDeductionDTO();
                    deductionCopy.setDeductionType(d.getDeductionType());
                    deductionCopy.setDeductionName(d.getDeductionName());
                    deductionCopy.setIsPercentage(d.getIsPercentage());
                    deductionCopy.setAmount(d.getAmount());
                    deductionCopy.setPercentageValue(d.getPercentageValue());
                    deductionCopy.setDescription(d.getDescription());
                    deductionCopy.setIsActive(d.getIsActive());
                    // Don't copy the ID - this is a new deduction
                    return deductionCopy;
                })
                .collect(java.util.stream.Collectors.toList()));

        // Open form with the copy for editing
        incomeSourceForm.setIncomeSourceWithDeductions(copy, copy.getDeductions());
        formDialog.open();
    }

    private void showAnalysisView() {
        contentLayout.removeAll();

        User user = getCurrentUser();
        List<IncomeSourceDTO> allSources = incomeSourceService.findByUserAsDto(user);

        VerticalLayout analysisContent = new VerticalLayout();
        analysisContent.setSpacing(true);
        analysisContent.setPadding(false);
        analysisContent.setWidthFull();

        // Monthly Breakdown Section
        analysisContent.add(createMonthlyBreakdownSection(allSources));

        // Category Breakdown Section
        analysisContent.add(createCategoryBreakdownSection(allSources));

        // Year Comparison Section
        analysisContent.add(createYearComparisonSection(allSources));

        contentLayout.add(analysisContent);
    }

    private VerticalLayout createMonthlyBreakdownSection(List<IncomeSourceDTO> allSources) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.setWidthFull();
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Monthly Breakdown" + (selectedYear != null ? " (" + selectedYear + ")" : " (All Time)"));
        sectionTitle.getStyle().set("margin-top", "0");

        // Filter by selected year
        List<IncomeSourceDTO> filteredSources = filterByYear(allSources);

        // Calculate monthly data
        List<MonthlyIncomeData> monthlyData = calculateMonthlyBreakdown(filteredSources);

        if (monthlyData.isEmpty()) {
            Span noData = new Span("No income data available for this period");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            section.add(sectionTitle, noData);
        } else {
            // Create a grid showing income by month
            Grid<MonthlyIncomeData> monthlyGrid = new Grid<>();
            monthlyGrid.setAllRowsVisible(true);
            monthlyGrid.setWidthFull();

            monthlyGrid.addColumn(MonthlyIncomeData::month).setHeader("Month").setAutoWidth(true);
            monthlyGrid.addColumn(data -> String.format("£%.2f", data.grossIncome())).setHeader("Gross Income").setAutoWidth(true);
            monthlyGrid.addColumn(data -> String.format("£%.2f", data.netIncome())).setHeader("Net Income").setAutoWidth(true);
            monthlyGrid.addColumn(MonthlyIncomeData::count).setHeader("# Entries").setAutoWidth(true);

            monthlyGrid.setItems(monthlyData);
            section.add(sectionTitle, monthlyGrid);
        }

        return section;
    }

    private List<MonthlyIncomeData> calculateMonthlyBreakdown(List<IncomeSourceDTO> sources) {
        java.util.Map<String, MonthlyIncomeData> monthlyMap = new java.util.LinkedHashMap<>();

        for (IncomeSourceDTO source : sources) {
            if (source.getStartDate() == null) continue;

            String monthKey = source.getStartDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
            BigDecimal gross = source.getGrossAmount() != null ? source.getGrossAmount() :
                    (source.getAmount() != null ? source.getAmount() : BigDecimal.ZERO);
            BigDecimal net = source.getNetAmount() != null ? source.getNetAmount() : gross;

            MonthlyIncomeData existing = monthlyMap.get(monthKey);
            if (existing != null) {
                monthlyMap.put(monthKey, new MonthlyIncomeData(
                        monthKey,
                        existing.grossIncome().add(gross),
                        existing.netIncome().add(net),
                        existing.count() + 1
                ));
            } else {
                monthlyMap.put(monthKey, new MonthlyIncomeData(monthKey, gross, net, 1));
            }
        }

        return new java.util.ArrayList<>(monthlyMap.values());
    }

    private VerticalLayout createCategoryBreakdownSection(List<IncomeSourceDTO> allSources) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.setWidthFull();
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Income by Category" + (selectedYear != null ? " (" + selectedYear + ")" : " (All Time)"));
        sectionTitle.getStyle().set("margin-top", "0");

        // Filter by selected year
        List<IncomeSourceDTO> filteredSources = filterByYear(allSources);

        // Group by category
        java.util.Map<String, BigDecimal> categoryTotals = new java.util.HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (IncomeSourceDTO source : filteredSources) {
            String category = source.getCategoryName() != null ? source.getCategoryName() : "Uncategorised";
            BigDecimal amount = source.getNetAmount() != null ? source.getNetAmount() :
                    (source.getAmount() != null ? source.getAmount() : BigDecimal.ZERO);
            categoryTotals.merge(category, amount, BigDecimal::add);
            grandTotal = grandTotal.add(amount);
        }

        if (categoryTotals.isEmpty()) {
            Span noData = new Span("No income data available for this period");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            section.add(sectionTitle, noData);
        } else {
            // Display as cards
            HorizontalLayout cardsLayout = new HorizontalLayout();
            cardsLayout.setSpacing(true);
            cardsLayout.setWidthFull();
            cardsLayout.getStyle().set("flex-wrap", "wrap");

            final BigDecimal total = grandTotal;
            categoryTotals.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> {
                        BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                                ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        cardsLayout.add(createCategoryCard(entry.getKey(), entry.getValue(), percentage));
                    });

            section.add(sectionTitle, cardsLayout);
        }

        return section;
    }

    private VerticalLayout createCategoryCard(String category, BigDecimal amount, BigDecimal percentage) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("200px");
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        Span categorySpan = new Span(category);
        categorySpan.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)");

        Span amountSpan = new Span(String.format("£%.2f", amount));
        amountSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-success-color)");

        Span percentSpan = new Span(String.format("%.1f%% of total", percentage));
        percentSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(categorySpan, amountSpan, percentSpan);
        return card;
    }

    private VerticalLayout createYearComparisonSection(List<IncomeSourceDTO> allSources) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.setWidthFull();
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Year Comparison");
        sectionTitle.getStyle().set("margin-top", "0");

        int currentYear = LocalDate.now().getYear();

        // Calculate totals for each year
        java.util.Map<Integer, BigDecimal> yearTotals = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        for (IncomeSourceDTO source : allSources) {
            if (source.getStartDate() == null) continue;
            int year = source.getStartDate().getYear();
            BigDecimal amount = source.getNetAmount() != null ? source.getNetAmount() :
                    (source.getAmount() != null ? source.getAmount() : BigDecimal.ZERO);
            yearTotals.merge(year, amount, BigDecimal::add);
        }

        if (yearTotals.isEmpty()) {
            Span noData = new Span("No income data available");
            noData.getStyle().set("color", "var(--lumo-secondary-text-color)");
            section.add(sectionTitle, noData);
        } else {
            HorizontalLayout yearCardsLayout = new HorizontalLayout();
            yearCardsLayout.setSpacing(true);

            Integer previousYear = null;
            BigDecimal previousYearTotal = null;

            for (java.util.Map.Entry<Integer, BigDecimal> entry : yearTotals.entrySet()) {
                String change = "";
                if (previousYearTotal != null && previousYearTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal diff = entry.getValue().subtract(previousYearTotal);
                    BigDecimal changePercent = diff.multiply(BigDecimal.valueOf(100))
                            .divide(previousYearTotal, 1, java.math.RoundingMode.HALF_UP);
                    change = (diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + changePercent + "% vs " + previousYear;
                }
                yearCardsLayout.add(createYearCard(entry.getKey(), entry.getValue(), change, entry.getKey() == currentYear));
                previousYear = entry.getKey();
                previousYearTotal = entry.getValue();
            }

            section.add(sectionTitle, yearCardsLayout);
        }

        return section;
    }

    private VerticalLayout createYearCard(int year, BigDecimal total, String change, boolean isCurrentYear) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("180px");
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", isCurrentYear ? "2px solid var(--lumo-primary-color)" : "1px solid var(--lumo-contrast-10pct)");

        Span yearSpan = new Span(String.valueOf(year) + (isCurrentYear ? " (Current)" : ""));
        yearSpan.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", isCurrentYear ? "var(--lumo-primary-color)" : "var(--lumo-body-text-color)");

        Span amountSpan = new Span(String.format("£%.2f", total));
        amountSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold");

        card.add(yearSpan, amountSpan);

        if (!change.isEmpty()) {
            Span changeSpan = new Span(change);
            changeSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", change.startsWith("+") ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
            card.add(changeSpan);
        }

        return card;
    }

    private record MonthlyIncomeData(String month, BigDecimal grossIncome, BigDecimal netIncome, int count) {}

    private void setupDialogs() {
        incomeSourceForm = new IncomeSourceForm(
                incomeSourceService,
                incomeDeductionService,
                authenticationContext,
                userRepository,
                categoryRepository
        );
        incomeSourceForm.setSaveListener(dto -> {
            formDialog.close();
            refreshData();
        });
        incomeSourceForm.setCancelListener(() -> formDialog.close());

        formDialog = new Dialog();
        formDialog.setCloseOnEsc(true);
        formDialog.setCloseOnOutsideClick(false);
        formDialog.setModal(true);
        formDialog.setWidth("700px");

        H3 formTitle = new H3("Income Source");
        formTitle.getStyle().set("margin", "0");

        VerticalLayout dialogLayout = new VerticalLayout(formTitle, incomeSourceForm);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        formDialog.add(dialogLayout);
    }

    private void openIncomeSourceForm(final IncomeSourceDTO dto) {
        if (dto != null) {
            incomeSourceForm.setIncomeSource(dto);
        } else {
            incomeSourceForm.clearForm();
        }
        formDialog.open();
    }

    private void deleteIncomeSource(final IncomeSourceDTO dto) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(true);
        confirmDialog.setCloseOnOutsideClick(false);

        H3 title = new H3("Delete Income Source");
        String identifier = (dto.getCategoryName() != null ? dto.getCategoryName() : "Income") +
                " - " + dto.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        Span message = new Span("Are you sure you want to delete \"" + identifier +
                "\"? This action cannot be undone.");

        Button confirmButton = new Button("Delete", event -> {
            try {
                incomeSourceService.deleteById(dto.getIncomeSourceId());
                showSuccessNotification("Income source deleted successfully");
                refreshData();
                confirmDialog.close();
            } catch (Exception e) {
                showErrorNotification("Error deleting income source: " + e.getMessage());
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Cancel", event -> confirmDialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(title, message, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        confirmDialog.add(dialogLayout);
        confirmDialog.open();
    }

    private void refreshData() {
        User user = getCurrentUser();

        // Get all income sources and filter by year if selected
        List<IncomeSourceDTO> allIncomeSources = incomeSourceService.findByUserAsDto(user);
        List<IncomeSourceDTO> filteredSources = filterByYear(allIncomeSources);

        // Calculate totals from filtered sources
        BigDecimal totalGrossIncome = filteredSources.stream()
                .filter(dto -> dto.getIsActive() != null && dto.getIsActive())
                .map(dto -> dto.getGrossAmount() != null ? dto.getGrossAmount() : dto.getAmount())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recurringIncome = filteredSources.stream()
                .filter(dto -> dto.getIsActive() != null && dto.getIsActive())
                .filter(dto -> dto.getIsRecurring() != null && dto.getIsRecurring())
                .map(dto -> dto.getGrossAmount() != null ? dto.getGrossAmount() : dto.getAmount())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetIncome = filteredSources.stream()
                .filter(dto -> dto.getIsActive() != null && dto.getIsActive())
                .map(dto -> dto.getNetAmount() != null ? dto.getNetAmount() : dto.getAmount())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        IncomeForecastDTO monthlyForecast = incomeForecastService.generateMonthlyForecast(user);

        postDeductionIncomeSpan.setText(String.format("£%.2f", totalNetIncome));
        totalIncomeSpan.setText(String.format("£%.2f", totalGrossIncome));
        recurringIncomeSpan.setText(String.format("£%.2f", recurringIncome));
        forecastedMonthlySpan.setText(String.format("£%.2f", monthlyForecast.getAverageMonthlyIncome()));

        refreshIncomeSourceGrid();
    }

    private List<IncomeSourceDTO> filterByYear(List<IncomeSourceDTO> sources) {
        if (selectedYear == null) {
            // "All Time" - no filtering
            return sources;
        }
        return sources.stream()
                .filter(dto -> dto.getStartDate() != null && dto.getStartDate().getYear() == selectedYear)
                .collect(java.util.stream.Collectors.toList());
    }

    private void refreshIncomeSourceGrid() {
        if (incomeSourceGrid != null) {
            User user = getCurrentUser();
            List<IncomeSourceDTO> incomeSources = incomeSourceService.findByUserAsDto(user);
            List<IncomeSourceDTO> filteredSources = filterByYear(incomeSources);
            incomeSourceGrid.setItems(filteredSources);
        }
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void showSuccessNotification(final String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(final String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
