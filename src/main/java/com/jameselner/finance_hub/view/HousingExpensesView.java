package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.HousingExpenseService;
import com.jameselner.finance_hub.view.components.HousingExpenseFormDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
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
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

@Route(value = "housing", layout = MainLayout.class)
@PageTitle("Housing Expenses | Finance Hub")
@PermitAll
public class HousingExpensesView extends VerticalLayout {

    private final HousingExpenseService housingExpenseService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private HousingExpenseFormDialog formDialog;
    private ComboBox<Month> monthFilter;
    private ComboBox<Integer> yearFilter;

    private Span yearTotalCard;
    private Span avgMonthlyCard;
    private Span housingRatioCard;
    private Span monthsTrackedCard;

    private VerticalLayout expensesContainer;

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
                createToolbar(),
                createExpensesContainer()
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
        header.addClassName("mobile-toolbar");

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        yearTotalCard = createSummaryCard("Year Total", "£0.00", "var(--lumo-primary-color)", VaadinIcon.CALC);
        avgMonthlyCard = createSummaryCard("Avg Monthly", "£0.00", "var(--lumo-success-color)", VaadinIcon.TRENDING_UP);
        housingRatioCard = createSummaryCard("% of Net Income", "0%", "var(--lumo-warning-color)", VaadinIcon.PIE_CHART);
        monthsTrackedCard = createSummaryCard("Months Tracked", "0", "var(--lumo-contrast-color)", VaadinIcon.CALENDAR);

        cardsLayout.add(
                createCardContainer(yearTotalCard),
                createCardContainer(avgMonthlyCard),
                createCardContainer(housingRatioCard),
                createCardContainer(monthsTrackedCard)
        );

        return cardsLayout;
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
        monthFilter = new ComboBox<>("Month");
        monthFilter.setItems(Month.values());
        monthFilter.setItemLabelGenerator(m -> m.getDisplayName(TextStyle.FULL, Locale.UK));
        monthFilter.setClearButtonVisible(true);
        monthFilter.setPlaceholder("All months");
        monthFilter.addValueChangeListener(event -> refreshExpenses());

        yearFilter = new ComboBox<>("Year");
        int currentYear = LocalDate.now().getYear();
        yearFilter.setItems(IntStream.rangeClosed(currentYear - 5, currentYear + 1).boxed().toList());
        yearFilter.setValue(currentYear);
        yearFilter.addValueChangeListener(event -> refreshData());

        HorizontalLayout toolbar = new HorizontalLayout(monthFilter, yearFilter);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private VerticalLayout createExpensesContainer() {
        expensesContainer = new VerticalLayout();
        expensesContainer.setPadding(false);
        expensesContainer.setSpacing(true);
        expensesContainer.setWidthFull();
        return expensesContainer;
    }

    private void setupFormDialog() {
        formDialog = new HousingExpenseFormDialog(housingExpenseService);
        formDialog.setSaveListener(expense -> refreshData());
    }

    private void openFormDialog(final HousingExpenseDTO expense) {
        User currentUser = getCurrentUser();
        formDialog.open(currentUser.getUserId(), expense);
    }

    private void openFormDialogForMonth(final LocalDate month) {
        User currentUser = getCurrentUser();
        HousingExpenseDTO dto = new HousingExpenseDTO();
        dto.setExpenseMonth(month);
        formDialog.open(currentUser.getUserId(), dto);
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

    private void copyMonthToNext(final LocalDate sourceMonth) {
        try {
            User currentUser = getCurrentUser();
            List<HousingExpenseDTO> copied = housingExpenseService.copyMonthToNext(currentUser.getUserId(), sourceMonth);

            LocalDate targetMonth = sourceMonth.plusMonths(1);
            String targetMonthName = targetMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + targetMonth.getYear();

            showSuccessNotification("Copied " + copied.size() + " expenses to " + targetMonthName);
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error copying expenses: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshExpenses();
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        Integer selectedYear = yearFilter.getValue();
        if (selectedYear == null) {
            selectedYear = LocalDate.now().getYear();
        }

        BigDecimal yearTotal = housingExpenseService.calculateTotalForYear(userId, selectedYear);
        BigDecimal avgMonthly = housingExpenseService.calculateAverageMonthlyForYear(userId, selectedYear);
        BigDecimal housingRatio = housingExpenseService.calculateHousingToIncomeRatioForYear(userId, selectedYear);
        long monthsTracked = housingExpenseService.countMonthsWithExpensesForYear(userId, selectedYear);

        updateCardValue(yearTotalCard, String.format("£%.2f", yearTotal));
        updateCardValue(avgMonthlyCard, String.format("£%.2f", avgMonthly));
        updateCardValue(housingRatioCard, String.format("%.1f%%", housingRatio));
        updateCardValue(monthsTrackedCard, String.valueOf(monthsTracked));
    }

    private void refreshExpenses() {
        expensesContainer.removeAll();

        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        Month selectedMonth = monthFilter.getValue();
        Integer selectedYear = yearFilter.getValue();

        if (selectedMonth != null && selectedYear != null) {
            // Show specific month only
            LocalDate month = LocalDate.of(selectedYear, selectedMonth, 1);
            List<HousingExpenseDTO> expenses = housingExpenseService.findByUserIdAndMonthAsDto(userId, month);
            if (!expenses.isEmpty()) {
                expensesContainer.add(createMonthSection(month, expenses));
            } else {
                expensesContainer.add(createEmptyMonthSection(month));
            }
        } else if (selectedYear != null) {
            // Show all months for the selected year that have expenses
            Map<String, List<HousingExpenseDTO>> grouped = housingExpenseService.getExpensesGroupedByMonth(userId);

            grouped.forEach((monthKey, expenses) -> {
                if (!expenses.isEmpty()) {
                    LocalDate expenseMonth = expenses.getFirst().getExpenseMonth();
                    if (expenseMonth.getYear() == selectedYear) {
                        expensesContainer.add(createMonthSection(expenseMonth, expenses));
                    }
                }
            });

            if (expensesContainer.getComponentCount() == 0) {
                Span noData = new Span("No housing expenses found for " + selectedYear);
                noData.getStyle()
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("padding", "2rem")
                        .set("text-align", "center");
                expensesContainer.add(noData);
            }
        }
    }

    private VerticalLayout createMonthSection(final LocalDate month, final List<HousingExpenseDTO> expenses) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Month header with total and actions
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + month.getYear();
        BigDecimal monthTotal = expenses.stream()
                .map(HousingExpenseDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        H4 monthTitle = new H4(monthName);
        monthTitle.getStyle().set("margin", "0");

        Span totalSpan = new Span(String.format("Total: £%.2f", monthTotal));
        totalSpan.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-color)");

        Button addToMonthButton = new Button(new Icon(VaadinIcon.PLUS));
        addToMonthButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        addToMonthButton.setTooltipText("Add expense to " + monthName);
        addToMonthButton.addClickListener(event -> openFormDialogForMonth(month));

        Button copyButton = new Button("Copy to Next Month", new Icon(VaadinIcon.COPY));
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        copyButton.addClickListener(event -> confirmCopyMonth(month));

        HorizontalLayout headerLeft = new HorizontalLayout(monthTitle, totalSpan);
        headerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLeft.setSpacing(true);

        HorizontalLayout headerRight = new HorizontalLayout(addToMonthButton, copyButton);
        headerRight.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(headerLeft, headerRight);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        section.add(header);

        // Expense items
        for (HousingExpenseDTO expense : expenses) {
            section.add(createExpenseRow(expense));
        }

        return section;
    }

    private VerticalLayout createEmptyMonthSection(final LocalDate month) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + month.getYear();

        H4 monthTitle = new H4(monthName);
        monthTitle.getStyle().set("margin", "0");

        Button addButton = new Button("Add First Expense", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.addClickListener(event -> openFormDialogForMonth(month));

        HorizontalLayout header = new HorizontalLayout(monthTitle, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span noExpenses = new Span("No expenses for this month");
        noExpenses.getStyle().set("color", "var(--lumo-secondary-text-color)");

        section.add(header, noExpenses);

        return section;
    }

    private HorizontalLayout createExpenseRow(final HousingExpenseDTO expense) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("padding", "0.5rem")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-s)");

        Span typeSpan = new Span(expense.getExpenseType().getDisplayName());
        typeSpan.getStyle()
                .set("flex", "1")
                .set("font-weight", "500");

        Span amountSpan = new Span(String.format("£%.2f", expense.getAmount()));
        amountSpan.getStyle()
                .set("font-weight", "bold")
                .set("min-width", "100px")
                .set("text-align", "right");

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.addClickListener(event -> openFormDialog(expense));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(event -> confirmDelete(expense));

        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(false);

        row.add(typeSpan, amountSpan, actions);

        return row;
    }

    private void confirmCopyMonth(final LocalDate sourceMonth) {
        LocalDate targetMonth = sourceMonth.plusMonths(1);
        String sourceMonthName = sourceMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + sourceMonth.getYear();
        String targetMonthName = targetMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + targetMonth.getYear();

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Copy Expenses");
        dialog.setText("Copy all expenses from " + sourceMonthName + " to " + targetMonthName + "?");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Copy");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(event -> copyMonthToNext(sourceMonth));

        dialog.open();
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
