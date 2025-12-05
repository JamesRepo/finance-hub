package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.IncomeForecastDTO;
import com.jameselner.finance_hub.dto.IncomeReportDTO;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.dto.IncomeVsExpenseDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.IncomeForecastService;
import com.jameselner.finance_hub.service.IncomeReportService;
import com.jameselner.finance_hub.service.IncomeSourceService;
import com.jameselner.finance_hub.view.components.IncomeSourceForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "income", layout = MainLayout.class)
@PageTitle("Income Tracking | Finance Hub")
@PermitAll
public class IncomeTrackingView extends VerticalLayout {

    private final IncomeSourceService incomeSourceService;
    private final IncomeForecastService incomeForecastService;
    private final IncomeReportService incomeReportService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private Span totalIncomeSpan;
    private Span recurringIncomeSpan;
    private Span activeSourcesSpan;
    private Span forecastedMonthlySpan;

    private VerticalLayout contentLayout;
    private Grid<IncomeSourceDTO> incomeSourceGrid;
    private VerticalLayout forecastLayout;
    private VerticalLayout reportsLayout;
    private VerticalLayout comparisonLayout;

    private IncomeSourceForm incomeSourceForm;
    private Dialog formDialog;

    private Tabs navigationTabs;
    private Tab sourcesTab;
    private Tab forecastTab;
    private Tab reportsTab;
    private Tab comparisonTab;

    public IncomeTrackingView(
            final IncomeSourceService incomeSourceService,
            final IncomeForecastService incomeForecastService,
            final IncomeReportService incomeReportService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository,
            final CategoryRepository categoryRepository
    ) {
        this.incomeSourceService = incomeSourceService;
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

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        cardsLayout.add(
                createSummaryCard("Total Active Income", "£0.00", VaadinIcon.MONEY, "total-income"),
                createSummaryCard("Recurring Income", "£0.00", VaadinIcon.REFRESH, "recurring-income"),
                createSummaryCard("Active Sources", "0", VaadinIcon.LIST, "active-sources"),
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
            case "active-sources":
                activeSourcesSpan = valueSpan;
                break;
            case "forecasted-monthly":
                forecastedMonthlySpan = valueSpan;
                break;
        }

        return card;
    }

    private Tabs createNavigationTabs() {
        sourcesTab = new Tab(new Icon(VaadinIcon.LIST), new Span("Income Sources"));
        forecastTab = new Tab(new Icon(VaadinIcon.TRENDING_UP), new Span("Forecast"));
        reportsTab = new Tab(new Icon(VaadinIcon.CHART), new Span("Reports"));
        comparisonTab = new Tab(new Icon(VaadinIcon.SPLIT), new Span("Income vs Expenses"));

        navigationTabs = new Tabs(sourcesTab, forecastTab, reportsTab, comparisonTab);
        navigationTabs.setWidthFull();

        navigationTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab == sourcesTab) {
                showSourcesView();
            } else if (selectedTab == forecastTab) {
                showForecastView();
            } else if (selectedTab == reportsTab) {
                showReportsView();
            } else if (selectedTab == comparisonTab) {
                showComparisonView();
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

        grid.addColumn(IncomeSourceDTO::getSourceName)
                .setHeader("Source Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAmount()))
                .setHeader("Amount")
                .setAutoWidth(true);

        grid.addColumn(dto -> dto.getIsRecurring() ? dto.getRecurrenceFrequency().getDisplayName() : "One-time")
                .setHeader("Frequency")
                .setAutoWidth(true);

        grid.addColumn(dto -> dto.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Start Date")
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
        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        editButton.getElement().setAttribute("title", "Edit");
        editButton.addClickListener(event -> openIncomeSourceForm(dto));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("title", "Delete");
        deleteButton.addClickListener(event -> deleteIncomeSource(dto));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(true);

        return actions;
    }

    private void showForecastView() {
        contentLayout.removeAll();

        User user = getCurrentUser();
        IncomeForecastDTO monthlyForecast = incomeForecastService.generateMonthlyForecast(user);
        IncomeForecastDTO quarterlyForecast = incomeForecastService.generateQuarterlyForecast(user);
        IncomeForecastDTO yearlyForecast = incomeForecastService.generateYearlyForecast(user);

        VerticalLayout forecastContent = new VerticalLayout();
        forecastContent.setSpacing(true);
        forecastContent.setPadding(false);

        forecastContent.add(
                createForecastSection("Next Month Forecast", monthlyForecast),
                createForecastSection("Next Quarter Forecast", quarterlyForecast),
                createForecastSection("Next Year Forecast", yearlyForecast)
        );

        contentLayout.add(forecastContent);
    }

    private VerticalLayout createForecastSection(final String title, final IncomeForecastDTO forecast) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle().set("margin-top", "0");

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setSpacing(true);

        statsLayout.add(
                createStatLabel("Total Forecasted:", String.format("£%.2f", forecast.getTotalForecastedIncome())),
                createStatLabel("Avg Monthly:", String.format("£%.2f", forecast.getAverageMonthlyIncome())),
                createStatLabel("Avg Weekly:", String.format("£%.2f", forecast.getAverageWeeklyIncome()))
        );

        section.add(sectionTitle, statsLayout);

        return section;
    }

    private Div createStatLabel(final String label, final String value) {
        Div container = new Div();
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(" " + value);
        valueSpan.getStyle().set("font-weight", "bold");

        container.add(labelSpan, valueSpan);
        return container;
    }

    private void showReportsView() {
        contentLayout.removeAll();

        User user = getCurrentUser();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);

        IncomeReportDTO report = incomeReportService.generateIncomeReport(user, startDate, endDate);

        VerticalLayout reportContent = new VerticalLayout();
        reportContent.setSpacing(true);
        reportContent.setPadding(true);
        reportContent.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 reportTitle = new H3("Income Report (Last 6 Months)");
        reportTitle.getStyle().set("margin-top", "0");

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);

        statsLayout.add(
                createReportCard("Total Income", String.format("£%.2f", report.getTotalIncome())),
                createReportCard("Recurring Income", String.format("£%.2f", report.getRecurringIncome())),
                createReportCard("One-time Income", String.format("£%.2f", report.getOneTimeIncome())),
                createReportCard("Avg Monthly", String.format("£%.2f", report.getAverageMonthlyIncome()))
        );

        reportContent.add(reportTitle, statsLayout);
        contentLayout.add(reportContent);
    }

    private VerticalLayout createReportCard(final String label, final String value) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("flex", "1");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private void showComparisonView() {
        contentLayout.removeAll();

        User user = getCurrentUser();
        IncomeVsExpenseDTO currentMonth = incomeReportService.compareCurrentMonth(user);
        IncomeVsExpenseDTO lastMonth = incomeReportService.compareLastMonth(user);
        IncomeVsExpenseDTO yearToDate = incomeReportService.compareYearToDate(user);

        VerticalLayout comparisonContent = new VerticalLayout();
        comparisonContent.setSpacing(true);
        comparisonContent.setPadding(false);

        comparisonContent.add(
                createComparisonSection("Current Month", currentMonth),
                createComparisonSection("Last Month", lastMonth),
                createComparisonSection("Year to Date", yearToDate)
        );

        contentLayout.add(comparisonContent);
    }

    private VerticalLayout createComparisonSection(final String title, final IncomeVsExpenseDTO comparison) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle().set("margin-top", "0");

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);

        statsLayout.add(
                createComparisonCard("Income", String.format("£%.2f", comparison.getTotalIncome()), "success"),
                createComparisonCard("Expenses", String.format("£%.2f", comparison.getTotalExpenses()), "error"),
                createComparisonCard("Net", String.format("£%.2f", comparison.getNetAmount()),
                        comparison.getIsPositive() ? "success" : "error"),
                createComparisonCard("Savings Rate", String.format("%.1f%%", comparison.getSavingsRate()),
                        comparison.getIsPositive() ? "primary" : "contrast")
        );

        Span statusBadge = new Span(getStatusLabel(comparison.getStatus()));
        statusBadge.getElement().getThemeList().add("badge");
        statusBadge.getElement().getThemeList().add(getStatusTheme(comparison.getStatus()));
        statusBadge.getStyle().set("margin-top", "0.5rem");

        section.add(sectionTitle, statsLayout, statusBadge);

        return section;
    }

    private VerticalLayout createComparisonCard(final String label, final String value, final String theme) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("flex", "1");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold");

        if (theme.equals("success")) {
            valueSpan.getStyle().set("color", "var(--lumo-success-color)");
        } else if (theme.equals("error")) {
            valueSpan.getStyle().set("color", "var(--lumo-error-color)");
        } else if (theme.equals("primary")) {
            valueSpan.getStyle().set("color", "var(--lumo-primary-color)");
        }

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private String getStatusLabel(final String status) {
        return switch (status) {
            case "EXCELLENT" -> "Excellent";
            case "GOOD" -> "Good";
            case "MODERATE" -> "Moderate";
            case "BREAK_EVEN" -> "Break Even";
            case "DEFICIT" -> "Deficit";
            default -> "Unknown";
        };
    }

    private String getStatusTheme(final String status) {
        return switch (status) {
            case "EXCELLENT", "GOOD" -> "success";
            case "MODERATE" -> "primary";
            case "BREAK_EVEN" -> "contrast";
            case "DEFICIT" -> "error";
            default -> "contrast";
        };
    }

    private void setupDialogs() {
        incomeSourceForm = new IncomeSourceForm(
                incomeSourceService,
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
        Span message = new Span("Are you sure you want to delete \"" + dto.getSourceName() +
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

        BigDecimal totalIncome = incomeSourceService.getTotalActiveIncomeByUser(user);
        BigDecimal recurringIncome = incomeSourceService.getTotalRecurringIncomeByUser(user);
        long activeSources = incomeSourceService.countActiveIncomeSourcesByUser(user);

        IncomeForecastDTO monthlyForecast = incomeForecastService.generateMonthlyForecast(user);

        totalIncomeSpan.setText(String.format("£%.2f", totalIncome != null ? totalIncome : BigDecimal.ZERO));
        recurringIncomeSpan.setText(String.format("£%.2f", recurringIncome != null ? recurringIncome : BigDecimal.ZERO));
        activeSourcesSpan.setText(String.valueOf(activeSources));
        forecastedMonthlySpan.setText(String.format("£%.2f", monthlyForecast.getAverageMonthlyIncome()));

        refreshIncomeSourceGrid();
    }

    private void refreshIncomeSourceGrid() {
        if (incomeSourceGrid != null) {
            User user = getCurrentUser();
            List<IncomeSourceDTO> incomeSources = incomeSourceService.findByUserAsDto(user);
            incomeSourceGrid.setItems(incomeSources);
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
