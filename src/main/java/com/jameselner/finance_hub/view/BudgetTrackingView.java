package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.BudgetService;
import com.jameselner.finance_hub.service.CategoryService;
import com.jameselner.finance_hub.view.components.BudgetForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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

@Route(value = "budgets", layout = MainLayout.class)
@PageTitle("Budget Tracking | Finance Hub")
@PermitAll
public class BudgetTrackingView extends VerticalLayout {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<BudgetDTO> budgetGrid;
    private BudgetForm budgetForm;
    private Dialog formDialog;
    private ComboBox<YearMonth> monthFilter;
    private YearMonth selectedMonth;

    public BudgetTrackingView(
            final BudgetService budgetService,
            final CategoryService categoryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createMonthFilter();
        createGrid();
        createFormDialog();

        add(createToolbar(), createSummaryCards(), budgetGrid);
        refreshGrid();
    }

    private void createHeader() {
        H2 header = new H2("Budget Tracking");
        add(header);
    }

    private HorizontalLayout createToolbar() {
        Button addButton = new Button("Add Budget");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openFormForNew());

        Button copyFromPreviousMonth = new Button("Copy from Previous Month");
        copyFromPreviousMonth.setIcon(VaadinIcon.COPY.create());
        copyFromPreviousMonth.addClickListener(event -> copyBudgetsFromPreviousMonth());

        HorizontalLayout leftSection = new HorizontalLayout(monthFilter);
        leftSection.setSpacing(true);
        leftSection.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout rightSection = new HorizontalLayout(copyFromPreviousMonth, addButton);
        rightSection.setSpacing(true);
        rightSection.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSection.addClassName("mobile-stack");

        HorizontalLayout toolbar = new HorizontalLayout(leftSection, rightSection);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.addClassName("mobile-toolbar");

        return toolbar;
    }

    private void createMonthFilter() {
        monthFilter = new ComboBox<>("Filter by Month");
        monthFilter.setWidth("200px");

        // Create list of last 24 months
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 24; i++) {
            months.add(current.minusMonths(i));
        }

        monthFilter.setItems(months);
        monthFilter.setItemLabelGenerator(month ->
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Set default to current month
        selectedMonth = YearMonth.now();
        monthFilter.setValue(selectedMonth);

        // Add listener to refresh grid when selection changes
        monthFilter.addValueChangeListener(event -> {
            selectedMonth = event.getValue();
            refreshGrid();
        });
    }

    private void createGrid() {
        budgetGrid = new Grid<>(BudgetDTO.class, false);
        budgetGrid.setSizeFull();

        budgetGrid.addColumn(new ComponentRenderer<>(this::createCategoryBadge))
                .setHeader("Category")
                .setAutoWidth(true)
                .setFlexGrow(1);

        budgetGrid.addColumn(dto -> "£" + dto.getAmount())
                .setHeader("Budget")
                .setAutoWidth(true);

        budgetGrid.addColumn(dto -> "£" + dto.getSpent())
                .setHeader("Spent")
                .setAutoWidth(true);

        budgetGrid.addColumn(dto -> "£" + dto.getRemaining())
                .setHeader("Remaining")
                .setAutoWidth(true);

        budgetGrid.addColumn(new ComponentRenderer<>(this::createProgressBar))
                .setHeader("Progress")
                .setFlexGrow(2);

        budgetGrid.addColumn(new ComponentRenderer<>(this::createStatusBadge))
                .setHeader("Status")
                .setAutoWidth(true);

        budgetGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        budgetGrid.addItemDoubleClickListener(event -> openFormForEdit(event.getItem()));
    }

    private HorizontalLayout createCategoryBadge(final BudgetDTO budget) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setPadding(false);

        // Create colored indicator dot
        Div colorDot = new Div();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", budget.getCategoryColorCode() != null ?
                        budget.getCategoryColorCode() : "#94a3b8")
                .set("flex-shrink", "0");

        Span categoryName = new Span(budget.getCategoryName());

        layout.add(colorDot, categoryName);
        return layout;
    }

    private VerticalLayout createProgressBar(final BudgetDTO budget) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();

        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();

        double percentage = budget.getPercentageUsed() / 100.0;
        progressBar.setValue(Math.min(percentage, 1.0));

        // Color-code the progress bar based on usage
        String color = getColorForPercentage(budget.getPercentageUsed());
        progressBar.getElement().getStyle().set("--vaadin-progress-value-color", color);

        Span percentageLabel = new Span(
                String.format("%.1f%%", budget.getPercentageUsed())
        );
        percentageLabel.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "#64748b");

        layout.add(progressBar, percentageLabel);
        return layout;
    }

    private Span createStatusBadge(final BudgetDTO budget) {
        Span badge = new Span();
        badge.getElement().getThemeList().add("badge");

        double percentage = budget.getPercentageUsed();
        BigDecimal overage = budget.getOverage();

        if (overage.compareTo(BigDecimal.ZERO) > 0) {
            badge.setText("Over Budget");
            badge.getElement().getThemeList().add("error");
            badge.getStyle()
                    .set("background-color", "#fecaca")
                    .set("color", "#991b1b")
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "500");
        } else if (percentage >= 80) {
            badge.setText("Warning");
            badge.getElement().getThemeList().add("contrast");
            badge.getStyle()
                    .set("background-color", "#fef3c7")
                    .set("color", "#92400e")
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "500");
        } else {
            badge.setText("On Track");
            badge.getElement().getThemeList().add("success");
            badge.getStyle()
                    .set("background-color", "#d1fae5")
                    .set("color", "#065f46")
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "9999px")
                    .set("font-size", "0.75rem")
                    .set("font-weight", "500");
        }

        return badge;
    }

    private HorizontalLayout createActionButtons(final BudgetDTO budget) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.addClassName("action-buttons");

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClassName("mobile-touch-target");
        editButton.getElement().setAttribute("aria-label", "Edit budget");
        editButton.addClickListener(event -> openFormForEdit(budget));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClassName("mobile-touch-target");
        deleteButton.getElement().setAttribute("aria-label", "Delete budget");
        deleteButton.addClickListener(event -> showDeleteConfirmation(budget));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private String getColorForPercentage(final double percentage) {
        if (percentage >= 100) {
            return "#dc2626"; // Red
        } else if (percentage >= 80) {
            return "#f59e0b"; // Yellow/Orange
        } else {
            return "#10b981"; // Green
        }
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.addClassName("mobile-stack");

        List<BudgetDTO> budgets = getCurrentMonthBudgets();

        // Calculate totals
        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = budgets.stream()
                .map(BudgetDTO::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = budgets.stream()
                .map(BudgetDTO::getRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overBudgetCount = budgets.stream()
                .filter(b -> b.getOverage().compareTo(BigDecimal.ZERO) > 0)
                .count();

        cards.add(
                createSummaryCard("Total Budgeted", "£" + totalBudgeted, VaadinIcon.WALLET, "#3b82f6"),
                createSummaryCard("Total Spent", "£" + totalSpent, VaadinIcon.CASH, "#8b5cf6"),
                createSummaryCard("Total Remaining", "£" + totalRemaining, VaadinIcon.PIGGY_BANK, "#10b981"),
                createSummaryCard("Over Budget", String.valueOf(overBudgetCount), VaadinIcon.WARNING, "#ef4444")
        );

        return cards;
    }

    private VerticalLayout createSummaryCard(
            final String title,
            final String value,
            final VaadinIcon icon,
            final String color
    ) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
                .set("background-color", "white")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "0.5rem")
                .set("padding", "1.5rem")
                .set("flex", "1");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "#64748b")
                .set("font-weight", "500");

        Icon iconComponent = icon.create();
        iconComponent.setColor(color);
        iconComponent.setSize("20px");

        header.add(titleSpan, iconComponent);

        H3 valueHeading = new H3(value);
        valueHeading.getStyle()
                .set("margin", "0.5rem 0 0 0")
                .set("font-size", "1.875rem")
                .set("font-weight", "700")
                .set("color", "#1e293b");

        card.add(header, valueHeading);
        return card;
    }

    private void createFormDialog() {
        formDialog = new Dialog();
        formDialog.setHeaderTitle("Budget");
        formDialog.setWidth("min(600px, 95vw)");
        formDialog.setMaxHeight("90vh");

        budgetForm = new BudgetForm(budgetService, categoryService, authenticationContext, userRepository);

        budgetForm.setSaveListener(budget -> {
            formDialog.close();
            refreshGrid();
        });

        budgetForm.setCancelListener(() -> formDialog.close());

        formDialog.add(budgetForm);
    }

    private void openFormForNew() {
        budgetForm.clearForm();
        formDialog.setHeaderTitle("Add Budget");
        formDialog.open();
    }

    private void openFormForEdit(final BudgetDTO budget) {
        budgetForm.setBudget(budget);
        formDialog.setHeaderTitle("Edit Budget");
        formDialog.open();
    }

    private void showDeleteConfirmation(final BudgetDTO budget) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Budget");
        dialog.setText(
                String.format("""
                                Are you sure you want to delete the budget for %s?
                                
                                Budget Amount: £%s
                                Period: %s to %s""",
                        budget.getCategoryName(),
                        budget.getAmount(),
                        budget.getStartDate(),
                        budget.getEndDate())
        );

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteBudget(budget));

        dialog.open();
    }

    private void deleteBudget(final BudgetDTO budget) {
        try {
            budgetService.deleteById(budget.getBudgetId());
            refreshGrid();
            showSuccessNotification("Budget deleted successfully");
        } catch (Exception e) {
            showErrorNotification("Error deleting budget: " + e.getMessage());
        }
    }

    private void copyBudgetsFromPreviousMonth() {
        YearMonth previousMonth = selectedMonth.minusMonths(1);
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        List<BudgetDTO> previousBudgets = budgetService.findByUserIdAndDateRange(
                userId,
                previousMonth.atDay(1),
                previousMonth.atEndOfMonth()
        );

        if (previousBudgets.isEmpty()) {
            showErrorNotification("No budgets found for " +
                    previousMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Copy Budgets");
        dialog.setText(
                String.format("Copy %d budget(s) from %s to %s?",
                        previousBudgets.size(),
                        previousMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
        );

        dialog.setCancelable(true);
        dialog.setConfirmText("Copy");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(e -> {
            int copiedCount = 0;
            for (BudgetDTO budget : previousBudgets) {
                try {
                    BudgetDTO newBudget = BudgetDTO.builder()
                            .userId(userId)
                            .categoryId(budget.getCategoryId())
                            .amount(budget.getAmount())
                            .periodType(budget.getPeriodType())
                            .startDate(selectedMonth.atDay(1))
                            .endDate(selectedMonth.atEndOfMonth())
                            .build();

                    budgetService.createBudgetFromDto(newBudget);
                    copiedCount++;
                } catch (Exception ex) {
                    // Skip if budget already exists for this category
                }
            }

            if (copiedCount > 0) {
                showSuccessNotification(copiedCount + " budget(s) copied successfully");
                refreshGrid();
            } else {
                showErrorNotification("Budgets already exist for selected categories in " +
                        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            }
        });

        dialog.open();
    }

    private void refreshGrid() {
        budgetGrid.setItems(getCurrentMonthBudgets());
    }

    private List<BudgetDTO> getCurrentMonthBudgets() {
        if (selectedMonth == null) {
            return new ArrayList<>();
        }

        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        return budgetService.findByUserIdAndDateRange(
                userId,
                selectedMonth.atDay(1),
                selectedMonth.atEndOfMonth()
        );
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
