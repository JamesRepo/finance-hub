package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.HousingExpenseService;
import com.jameselner.finance_hub.view.components.HousingExpenseFormDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Route(value = "housing", layout = MainLayout.class)
@PageTitle("Housing Expenses | Finance Hub")
@PermitAll
public class HousingExpensesView extends VerticalLayout {

    private final HousingExpenseService housingExpenseService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<HousingExpenseDTO> grid;
    private HousingExpenseFormDialog formDialog;
    private ComboBox<String> filterCombo;

    private Span totalMonthlyCard;
    private Span totalAnnualCard;
    private Span housingRatioCard;
    private Span activeCountCard;
    private Span utilitiesCard;

    private VerticalLayout historicalSection;

    public HousingExpensesView(
            final HousingExpenseService housingExpenseService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.housingExpenseService = housingExpenseService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createSummaryCards(),
                createUtilitiesSection(),
                createToolbar(),
                createGrid(),
                createHistoricalSection()
        );

        setupFormDialog();
        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Housing Expenses");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Add Expense", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openFormDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        totalMonthlyCard = createSummaryCard("Total Monthly", "£0.00", "var(--lumo-primary-color)", VaadinIcon.CALC);
        totalAnnualCard = createSummaryCard("Total Annual", "£0.00", "var(--lumo-success-color)", VaadinIcon.CALENDAR);
        housingRatioCard = createSummaryCard("Housing Ratio", "0%", "var(--lumo-warning-color)", VaadinIcon.PIE_CHART);
        activeCountCard = createSummaryCard("Active Expenses", "0", "var(--lumo-contrast-color)", VaadinIcon.LIST);

        cardsLayout.add(
                createCardContainer(totalMonthlyCard),
                createCardContainer(totalAnnualCard),
                createCardContainer(housingRatioCard),
                createCardContainer(activeCountCard)
        );

        return cardsLayout;
    }

    private VerticalLayout createUtilitiesSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Utilities Breakdown");
        sectionTitle.getStyle().set("margin", "0 0 0.5rem 0");

        utilitiesCard = createSummaryCard("Monthly Utilities", "£0.00", "var(--lumo-primary-color)", VaadinIcon.PLUG);

        section.add(sectionTitle, createCardContainer(utilitiesCard));

        return section;
    }

    private Div createCardContainer(final Span card) {
        Div container = new Div(card);
        container.getStyle()
                .set("flex", "1")
                .set("min-width", "200px");
        return container;
    }

    private Span createSummaryCard(final String label, final String value, final String color, final VaadinIcon icon) {
        Span card = new Span();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("padding", "1rem")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);

        return card;
    }

    private HorizontalLayout createToolbar() {
        filterCombo = new ComboBox<>("Filter by Type");
        filterCombo.setItems("All", "Rent", "Mortgage", "Property Tax", "Insurance",
                "Utilities", "Maintenance", "HOA Fees", "Other");
        filterCombo.setValue("All");
        filterCombo.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(filterCombo);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private Grid<HousingExpenseDTO> createGrid() {
        grid = new Grid<>(HousingExpenseDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("400px");

        grid.addColumn(dto -> dto.getExpenseType().getDisplayName())
                .setHeader("Type")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAmount()))
                .setHeader("Amount")
                .setAutoWidth(true);

        grid.addColumn(dto -> dto.getFrequency().getDisplayName())
                .setHeader("Frequency")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getMonthlyEquivalent()))
                .setHeader("Monthly")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAnnualEquivalent()))
                .setHeader("Annual")
                .setAutoWidth(true);

        grid.addColumn(HousingExpenseDTO::getStartDate)
                .setHeader("Start Date")
                .setAutoWidth(true);

        grid.addColumn(HousingExpenseDTO::getEndDate)
                .setHeader("End Date")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Span badge = new Span(dto.getIsActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(dto.getIsActive() ? "success" : "contrast");
            return badge;
        })
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openFormDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> confirmDelete(dto));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private VerticalLayout createHistoricalSection() {
        historicalSection = new VerticalLayout();
        historicalSection.setPadding(true);
        historicalSection.setSpacing(true);
        historicalSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H3 sectionTitle = new H3("Historical Housing Costs (Last 12 Months)");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");

        historicalSection.add(sectionTitle);

        return historicalSection;
    }

    private void setupFormDialog() {
        formDialog = new HousingExpenseFormDialog(housingExpenseService);
        formDialog.setSaveListener(expense -> refreshData());
    }

    private void openFormDialog(final HousingExpenseDTO expense) {
        User currentUser = getCurrentUser();
        formDialog.open(currentUser.getUserId(), expense);
    }

    private void confirmDelete(final HousingExpenseDTO expense) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Housing Expense");
        dialog.setText("Are you sure you want to delete this housing expense? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteExpense(expense));

        dialog.open();
    }

    private void deleteExpense(final HousingExpenseDTO expense) {
        try {
            housingExpenseService.deleteById(expense.getExpenseId());
            showSuccessNotification("Housing expense deleted successfully");
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting housing expense: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshUtilities();
        refreshGrid();
        refreshHistoricalData();
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        BigDecimal totalMonthly = housingExpenseService.calculateTotalMonthlyHousingCosts(userId);
        BigDecimal totalAnnual = housingExpenseService.calculateTotalAnnualHousingCosts(userId);
        BigDecimal housingRatio = housingExpenseService.calculateHousingToIncomeRatio(userId);
        long activeCount = housingExpenseService.countActiveExpenses(userId);

        updateCardValue(totalMonthlyCard, String.format("£%.2f", totalMonthly));
        updateCardValue(totalAnnualCard, String.format("£%.2f", totalAnnual));
        updateCardValue(housingRatioCard, String.format("%.1f%%", housingRatio));
        updateCardValue(activeCountCard, String.valueOf(activeCount));
    }

    private void refreshUtilities() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        BigDecimal utilitiesTotal = housingExpenseService.calculateTotalMonthlyUtilities(userId);
        updateCardValue(utilitiesCard, String.format("£%.2f", utilitiesTotal));
    }

    private void refreshGrid() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        String filter = filterCombo.getValue();
        List<HousingExpenseDTO> expenses;

        if ("All".equals(filter)) {
            expenses = housingExpenseService.findByUserIdAsDto(userId);
        } else {
            HousingExpenseType type = mapFilterToType(filter);
            expenses = housingExpenseService.findByUserIdAndTypeAsDto(userId, type);
        }

        grid.setItems(expenses);
    }

    private void refreshHistoricalData() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        Map<String, BigDecimal> historicalCosts = housingExpenseService.getHistoricalHousingCosts(userId, 12);

        // Clear existing content except title
        historicalSection.removeAll();
        H3 sectionTitle = new H3("Historical Housing Costs (Last 12 Months)");
        sectionTitle.getStyle().set("margin", "0 0 1rem 0");
        historicalSection.add(sectionTitle);

        // Create a grid layout for historical data
        historicalCosts.forEach((month, amount) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            row.getStyle().set("padding", "0.5rem 0");

            Span monthLabel = new Span(month);
            monthLabel.getStyle().set("font-weight", "500");

            Span amountLabel = new Span(String.format("£%.2f", amount));
            amountLabel.getStyle()
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-primary-text-color)");

            row.add(monthLabel, amountLabel);
            historicalSection.add(row);
        });
    }

    private HousingExpenseType mapFilterToType(final String filter) {
        return switch (filter) {
            case "Rent" -> HousingExpenseType.RENT;
            case "Mortgage" -> HousingExpenseType.MORTGAGE;
            case "Council Tax" -> HousingExpenseType.COUNCIL_TAX;
            case "Energy" -> HousingExpenseType.ENERGY;
            case "Internet" -> HousingExpenseType.INTERNET;
            case "Water" -> HousingExpenseType.WATER;
            case "Insurance" -> HousingExpenseType.INSURANCE;
            case "Utilities" -> HousingExpenseType.UTILITIES;
            case "Maintenance" -> HousingExpenseType.MAINTENANCE;
            case "HOA Fees" -> HousingExpenseType.HOA_FEES;
            default -> HousingExpenseType.OTHER;
        };
    }

    private void updateCardValue(final Span card, final String newValue) {
        // The value is the third child (icon, label, value)
        card.getChildren()
                .skip(2)
                .findFirst()
                .ifPresent(component -> ((Span) component).setText(newValue));
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
