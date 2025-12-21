package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.dto.HolidayExpenseDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.HolidayExpenseService;
import com.jameselner.finance_hub.service.HolidayService;
import com.jameselner.finance_hub.view.components.HolidayExpenseFormDialog;
import com.jameselner.finance_hub.view.components.HolidayFormDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route(value = "holidays", layout = MainLayout.class)
@PageTitle("Holidays | Finance Hub")
@PermitAll
public class HolidaysView extends VerticalLayout {

    private final HolidayService holidayService;
    private final HolidayExpenseService holidayExpenseService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<HolidayDTO> holidaysGrid;
    private Grid<HolidayExpenseDTO> expensesGrid;
    private HolidayFormDialog holidayFormDialog;
    private HolidayExpenseFormDialog expenseFormDialog;

    private Span totalBudgetCard;
    private Span totalSpentCard;
    private Span activeHolidaysCard;
    private Span budgetRemainingCard;

    private HolidayDTO selectedHoliday;
    private VerticalLayout expensesSection;

    public HolidaysView(
            final HolidayService holidayService,
            final HolidayExpenseService holidayExpenseService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.holidayService = holidayService;
        this.holidayExpenseService = holidayExpenseService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createSummaryCards(),
                createHolidaysSection(),
                createExpensesSection()
        );

        setupDialogs();
        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Holidays");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Add Holiday", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openHolidayDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("mobile-toolbar");

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        totalBudgetCard = createSummaryCard("Total Budget", "£0.00", "var(--lumo-primary-color)", VaadinIcon.PIGGY_BANK);
        totalSpentCard = createSummaryCard("Total Spent", "£0.00", "var(--lumo-error-color)", VaadinIcon.CREDIT_CARD);
        budgetRemainingCard = createSummaryCard("Budget Remaining", "£0.00", "var(--lumo-success-color)", VaadinIcon.MONEY);
        activeHolidaysCard = createSummaryCard("Active Holidays", "0", "var(--lumo-contrast-color)", VaadinIcon.AIRPLANE);

        cardsLayout.add(
                createCardContainer(totalBudgetCard),
                createCardContainer(totalSpentCard),
                createCardContainer(budgetRemainingCard),
                createCardContainer(activeHolidaysCard)
        );

        return cardsLayout;
    }

    private VerticalLayout createHolidaysSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H3 sectionTitle = new H3("Your Holidays");
        sectionTitle.getStyle().set("margin", "0");

        holidaysGrid = createHolidaysGrid();

        section.add(sectionTitle, holidaysGrid);
        return section;
    }

    private Grid<HolidayDTO> createHolidaysGrid() {
        Grid<HolidayDTO> grid = new Grid<>(HolidayDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("400px");

        grid.addColumn(HolidayDTO::getName)
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(HolidayDTO::getDestination)
                .setHeader("Destination")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(HolidayDTO::getStartDate)
                .setHeader("Start Date")
                .setAutoWidth(true);

        grid.addColumn(HolidayDTO::getEndDate)
                .setHeader("End Date")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("%d days", dto.getNumberOfDays() != null ? dto.getNumberOfDays() : 0))
                .setHeader("Duration")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getBudget()))
                .setHeader("Budget")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getTotalSpent() != null ? dto.getTotalSpent() : BigDecimal.ZERO))
                .setHeader("Spent")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getBudgetRemaining() != null ? dto.getBudgetRemaining() : dto.getBudget()))
                .setHeader("Remaining")
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
            Button viewButton = new Button(new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            viewButton.addClickListener(event -> selectHoliday(dto));

            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openHolidayDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> confirmDeleteHoliday(dto));

            HorizontalLayout actions = new HorizontalLayout(viewButton, editButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private VerticalLayout createExpensesSection() {
        expensesSection = new VerticalLayout();
        expensesSection.setPadding(true);
        expensesSection.setSpacing(true);
        expensesSection.setVisible(false);
        expensesSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        return expensesSection;
    }

    private Grid<HolidayExpenseDTO> createExpensesGrid() {
        Grid<HolidayExpenseDTO> grid = new Grid<>(HolidayExpenseDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("300px");

        grid.addColumn(dto -> dto.getExpenseType().getDisplayName())
                .setHeader("Type")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(HolidayExpenseDTO::getDescription)
                .setHeader("Description")
                .setAutoWidth(true)
                .setFlexGrow(2);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAmount()))
                .setHeader("Amount")
                .setAutoWidth(true);

        grid.addColumn(HolidayExpenseDTO::getExpenseDate)
                .setHeader("Date")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openExpenseDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> confirmDeleteExpense(dto));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
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

    private void setupDialogs() {
        holidayFormDialog = new HolidayFormDialog(holidayService);
        holidayFormDialog.setSaveListener(holiday -> refreshData());

        expenseFormDialog = new HolidayExpenseFormDialog(holidayExpenseService);
        expenseFormDialog.setSaveListener(expense -> {
            if (selectedHoliday != null) {
                selectHoliday(selectedHoliday);
            }
            refreshData();
        });
    }

    private void openHolidayDialog(final HolidayDTO holiday) {
        User currentUser = getCurrentUser();
        holidayFormDialog.open(currentUser.getUserId(), holiday);
    }

    private void openExpenseDialog(final HolidayExpenseDTO expense) {
        if (selectedHoliday != null) {
            expenseFormDialog.open(selectedHoliday.getHolidayId(), expense);
        }
    }

    private void selectHoliday(final HolidayDTO holiday) {
        this.selectedHoliday = holiday;
        expensesSection.removeAll();
        expensesSection.setVisible(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 title = new H3(holiday.getName() + " - Expenses");
        title.getStyle().set("margin", "0");

        Button addExpenseButton = new Button("Add Expense", new Icon(VaadinIcon.PLUS));
        addExpenseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addExpenseButton.addClickListener(event -> openExpenseDialog(null));

        header.add(title, addExpenseButton);

        // Budget summary
        VerticalLayout budgetSummary = new VerticalLayout();
        budgetSummary.setPadding(false);
        budgetSummary.setSpacing(false);

        HorizontalLayout budgetRow = new HorizontalLayout();
        budgetRow.setWidthFull();
        budgetRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span budgetLabel = new Span("Budget:");
        Span budgetValue = new Span(String.format("£%.2f", holiday.getBudget()));
        budgetValue.getStyle().set("font-weight", "bold");

        budgetRow.add(budgetLabel, budgetValue);

        HorizontalLayout spentRow = new HorizontalLayout();
        spentRow.setWidthFull();
        spentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span spentLabel = new Span("Spent:");
        Span spentValue = new Span(String.format("£%.2f",
                holiday.getTotalSpent() != null ? holiday.getTotalSpent() : BigDecimal.ZERO));
        spentValue.getStyle().set("font-weight", "bold").set("color", "var(--lumo-error-color)");

        spentRow.add(spentLabel, spentValue);

        HorizontalLayout remainingRow = new HorizontalLayout();
        remainingRow.setWidthFull();
        remainingRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span remainingLabel = new Span("Remaining:");
        BigDecimal remaining = holiday.getBudgetRemaining() != null ?
                holiday.getBudgetRemaining() : holiday.getBudget();
        Span remainingValue = new Span(String.format("£%.2f", remaining));
        remainingValue.getStyle().set("font-weight", "bold")
                .set("color", remaining.compareTo(BigDecimal.ZERO) >= 0 ?
                        "var(--lumo-success-color)" : "var(--lumo-error-color)");

        remainingRow.add(remainingLabel, remainingValue);

        budgetSummary.add(budgetRow, spentRow, remainingRow);

        expensesGrid = createExpensesGrid();
        refreshExpensesGrid();

        expensesSection.add(header, budgetSummary, expensesGrid);
    }

    private void confirmDeleteHoliday(final HolidayDTO holiday) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Holiday");
        dialog.setText("Are you sure you want to delete this holiday and all its expenses? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteHoliday(holiday));

        dialog.open();
    }

    private void deleteHoliday(final HolidayDTO holiday) {
        try {
            holidayService.deleteById(holiday.getHolidayId());
            showSuccessNotification("Holiday deleted successfully");
            if (selectedHoliday != null && selectedHoliday.getHolidayId().equals(holiday.getHolidayId())) {
                selectedHoliday = null;
                expensesSection.setVisible(false);
            }
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting holiday: " + e.getMessage());
        }
    }

    private void confirmDeleteExpense(final HolidayExpenseDTO expense) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Expense");
        dialog.setText("Are you sure you want to delete this expense? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteExpense(expense));

        dialog.open();
    }

    private void deleteExpense(final HolidayExpenseDTO expense) {
        try {
            holidayExpenseService.deleteById(expense.getExpenseId());
            showSuccessNotification("Expense deleted successfully");
            if (selectedHoliday != null) {
                selectHoliday(selectedHoliday);
            }
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting expense: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshHolidaysGrid();
        if (selectedHoliday != null) {
            // Refresh the selected holiday data
            holidayService.findByIdAsDto(selectedHoliday.getHolidayId())
                    .ifPresent(this::selectHoliday);
        }
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        BigDecimal totalBudget = holidayService.calculateTotalHolidayBudgets(userId);
        BigDecimal totalSpent = holidayService.calculateTotalHolidaySpending(userId);
        BigDecimal budgetRemaining = totalBudget.subtract(totalSpent);
        long activeHolidays = holidayService.countActiveHolidays(userId);

        updateCardValue(totalBudgetCard, String.format("£%.2f", totalBudget));
        updateCardValue(totalSpentCard, String.format("£%.2f", totalSpent));
        updateCardValue(budgetRemainingCard, String.format("£%.2f", budgetRemaining));
        updateCardValue(activeHolidaysCard, String.valueOf(activeHolidays));
    }

    private void refreshHolidaysGrid() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        List<HolidayDTO> holidays = holidayService.findByUserIdAsDto(userId);
        holidaysGrid.setItems(holidays);
    }

    private void refreshExpensesGrid() {
        if (selectedHoliday != null && expensesGrid != null) {
            List<HolidayExpenseDTO> expenses = holidayExpenseService.findByHolidayIdAsDto(selectedHoliday.getHolidayId());
            expensesGrid.setItems(expenses);
        }
    }

    private void updateCardValue(final Span card, final String newValue) {
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
