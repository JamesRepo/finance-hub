package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.DebtPayoffPlan;
import com.jameselner.finance_hub.dto.DebtPayoffProjection;
import com.jameselner.finance_hub.repository.DebtRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.DebtPaymentService;
import com.jameselner.finance_hub.service.DebtPayoffCalculatorService;
import com.jameselner.finance_hub.service.DebtService;
import com.jameselner.finance_hub.view.components.DebtForm;
import com.jameselner.finance_hub.view.components.DebtPaymentDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "debts", layout = MainLayout.class)
@PageTitle("Debt Management | Finance Hub")
@PermitAll
public class DebtManagementView extends VerticalLayout {

    private final DebtService debtService;
    private final DebtPaymentService debtPaymentService;
    private final DebtPayoffCalculatorService payoffCalculatorService;
    private final DebtRepository debtRepository;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<DebtDTO> debtGrid;
    private DebtForm debtForm;
    private Dialog formDialog;
    private DebtPaymentDialog paymentDialog;

    public DebtManagementView(
            final DebtService debtService,
            final DebtPaymentService debtPaymentService,
            final DebtPayoffCalculatorService payoffCalculatorService,
            final DebtRepository debtRepository,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.debtService = debtService;
        this.debtPaymentService = debtPaymentService;
        this.payoffCalculatorService = payoffCalculatorService;
        this.debtRepository = debtRepository;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createGrid();
        createFormDialog();
        createPaymentDialog();

        add(createToolbar(), createSummaryCards(), debtGrid);
        refreshGrid();
    }

    private void createHeader() {
        H2 header = new H2("Debt Management");
        add(header);
    }

    private HorizontalLayout createToolbar() {
        Button addButton = new Button("Add Debt");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setIcon(VaadinIcon.PLUS.create());
        addButton.addClickListener(event -> openFormForNew());

        Button calculatePayoffButton = new Button("Calculate Payoff Plan");
        calculatePayoffButton.setIcon(VaadinIcon.CALC.create());
        calculatePayoffButton.addClickListener(event -> showPayoffProjection());

        HorizontalLayout toolbar = new HorizontalLayout(addButton, calculatePayoffButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.START);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);
        toolbar.addClassName("mobile-toolbar");

        return toolbar;
    }

    private void createGrid() {
        debtGrid = new Grid<>(DebtDTO.class, false);
        debtGrid.setSizeFull();

        debtGrid.addColumn(DebtDTO::getDebtName)
                .setHeader("Debt Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        debtGrid.addColumn(DebtDTO::getDebtType)
                .setHeader("Type")
                .setAutoWidth(true);

        debtGrid.addColumn(dto -> "£" + dto.getCurrentBalance())
                .setHeader("Balance")
                .setAutoWidth(true);

        debtGrid.addColumn(dto -> dto.getInterestRate() + "%")
                .setHeader("APR")
                .setAutoWidth(true);

        debtGrid.addColumn(dto -> dto.getMinimumPayment() != null ? "£" + dto.getMinimumPayment() : "N/A")
                .setHeader("Min. Payment")
                .setAutoWidth(true);

        debtGrid.addColumn(dto -> "£" + debtPaymentService.getLastMonthInterestPaidByDebt(dto.getDebtId()))
                .setHeader("Last Month Interest")
                .setAutoWidth(true);

        debtGrid.addColumn(new ComponentRenderer<>(this::createProgressBar))
                .setHeader("Progress")
                .setFlexGrow(2);

        debtGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        debtGrid.addItemDoubleClickListener(event -> openFormForEdit(event.getItem()));
    }

    private VerticalLayout createProgressBar(final DebtDTO debt) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();

        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();

        BigDecimal principalAmount = debt.getPrincipalAmount();
        BigDecimal currentBalance = debt.getCurrentBalance();

        double paidPercentage = 0;
        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal paidAmount = principalAmount.subtract(currentBalance);
            paidPercentage = paidAmount.divide(principalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        progressBar.setValue(Math.max(0, Math.min(paidPercentage / 100.0, 1.0)));
        progressBar.getElement().getStyle().set("--vaadin-progress-value-color", "var(--finance-secondary)");

        Span progressLabel = new Span(
                String.format("%.1f%% paid off", paidPercentage)
        );
        progressLabel.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "var(--finance-text-secondary)");

        layout.add(progressBar, progressLabel);
        return layout;
    }

    private HorizontalLayout createActionButtons(final DebtDTO debt) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.addClassName("action-buttons");

        Button paymentButton = new Button(new Icon(VaadinIcon.MONEY));
        paymentButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
        paymentButton.addClassName("mobile-touch-target");
        paymentButton.getElement().setAttribute("aria-label", "Record payment");
        paymentButton.addClickListener(event -> openPaymentDialog(debt));

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClassName("mobile-touch-target");
        editButton.getElement().setAttribute("aria-label", "Edit debt");
        editButton.addClickListener(event -> openFormForEdit(debt));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClassName("mobile-touch-target");
        deleteButton.getElement().setAttribute("aria-label", "Delete debt");
        deleteButton.addClickListener(event -> showDeleteConfirmation(debt));

        layout.add(paymentButton, editButton, deleteButton);
        return layout;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.addClassName("mobile-stack");

        List<DebtDTO> debts = getAllDebts();

        BigDecimal totalDebt = debts.stream()
                .map(DebtDTO::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = debts.stream()
                .map(debt -> debt.getPrincipalAmount().subtract(debt.getCurrentBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgInterestRate = debts.isEmpty() ? BigDecimal.ZERO :
                debts.stream()
                        .map(DebtDTO::getInterestRate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(debts.size()), 2, RoundingMode.HALF_UP);

        BigDecimal lastMonthInterest = debts.stream()
                .map(debt -> debtPaymentService.getLastMonthInterestPaidByDebt(debt.getDebtId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long debtCount = debts.size();

        cards.add(
                createSummaryCard("Total Debt", "£" + totalDebt, VaadinIcon.CREDIT_CARD, "var(--finance-danger)"),
                createSummaryCard("Total Paid", "£" + totalPaid, VaadinIcon.CHECK_CIRCLE, "var(--finance-secondary)"),
                createSummaryCard("Avg. Interest Rate", avgInterestRate + "%", VaadinIcon.TRENDING_UP, "var(--finance-warning)"),
                createSummaryCard("Last Month Interest", "£" + lastMonthInterest, VaadinIcon.MONEY, "var(--finance-warning)"),
                createSummaryCard("Number of Debts", String.valueOf(debtCount), VaadinIcon.LIST, "var(--finance-primary)")
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
                .set("background-color", "var(--finance-card-bg)")
                .set("border", "1px solid var(--finance-border)")
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
                .set("color", "var(--finance-text-secondary)")
                .set("font-weight", "500");

        Icon iconComponent = icon.create();
        iconComponent.getStyle().set("color", color);
        iconComponent.setSize("20px");

        header.add(titleSpan, iconComponent);

        H3 valueHeading = new H3(value);
        valueHeading.getStyle()
                .set("margin", "0.5rem 0 0 0")
                .set("font-size", "1.875rem")
                .set("font-weight", "700")
                .set("color", "var(--finance-text-primary)");

        card.add(header, valueHeading);
        return card;
    }

    private void createFormDialog() {
        formDialog = new Dialog();
        formDialog.setHeaderTitle("Debt");
        formDialog.setWidth("min(800px, 95vw)");
        formDialog.setMaxHeight("90vh");

        debtForm = new DebtForm(debtService, authenticationContext, userRepository);

        debtForm.setSaveListener(debt -> {
            formDialog.close();
            refreshGrid();
        });

        debtForm.setCancelListener(() -> formDialog.close());

        formDialog.add(debtForm);
    }

    private void createPaymentDialog() {
        paymentDialog = new DebtPaymentDialog(debtPaymentService);
    }

    private void openFormForNew() {
        debtForm.clearForm();
        formDialog.setHeaderTitle("Add Debt");
        formDialog.open();
    }

    private void openFormForEdit(final DebtDTO debt) {
        debtForm.setDebt(debt);
        formDialog.setHeaderTitle("Edit Debt");
        formDialog.open();
    }

    private void openPaymentDialog(final DebtDTO debt) {
        paymentDialog.openForDebt(debt, this::refreshGrid);
    }

    private void showDeleteConfirmation(final DebtDTO debt) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Debt");
        dialog.setText(
                String.format("""
                                Are you sure you want to delete %s?
                                
                                Current Balance: £%s
                                Interest Rate: %s%%""",
                        debt.getDebtName(),
                        debt.getCurrentBalance(),
                        debt.getInterestRate())
        );

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteDebt(debt));

        dialog.open();
    }

    private void deleteDebt(final DebtDTO debt) {
        try {
            debtService.deleteById(debt.getDebtId());
            refreshGrid();
            showSuccessNotification("Debt deleted successfully");
        } catch (Exception e) {
            showErrorNotification("Error deleting debt: " + e.getMessage());
        }
    }

    private void showPayoffProjection() {
        List<DebtDTO> debts = getAllDebts();

        if (debts.isEmpty()) {
            showErrorNotification("No debts to calculate payoff plan");
            return;
        }

        Dialog calculatorDialog = new Dialog();
        calculatorDialog.setHeaderTitle("Debt Payoff Calculator");
        calculatorDialog.setWidth("900px");
        calculatorDialog.setMaxWidth("95%");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        BigDecimalField monthlyPaymentField = new BigDecimalField("Total Monthly Payment");
        monthlyPaymentField.setPrefixComponent(new Span("£"));
        monthlyPaymentField.setHelperText("How much can you pay per month towards all debts?");
        monthlyPaymentField.setWidth("300px");

        BigDecimal totalMinimumPayment = debts.stream()
                .map(DebtDTO::getMinimumPayment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Set a sensible default: minimum payments * 1.2, or 100 if no minimum payments are set
        BigDecimal defaultPayment = totalMinimumPayment.compareTo(BigDecimal.ZERO) > 0
                ? totalMinimumPayment.multiply(BigDecimal.valueOf(1.2)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);
        monthlyPaymentField.setValue(defaultPayment);

        Button calculateButton = new Button("Calculate");
        calculateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout resultsLayout = new VerticalLayout();
        resultsLayout.setSpacing(true);
        resultsLayout.setPadding(false);

        // Helper to run the calculation
        Runnable runCalculation = () -> {
            try {
                BigDecimal monthlyPayment = monthlyPaymentField.getValue();
                if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorNotification("Monthly payment must be greater than zero");
                    return;
                }

                List<Debt> debtEntities = debts.stream()
                        .map(dto -> debtRepository.findById(dto.getDebtId()).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // Ensure all debts have a minimum payment for calculation (use 0 if null)
                for (Debt debt : debtEntities) {
                    if (debt.getMinimumPayment() == null) {
                        debt.setMinimumPayment(BigDecimal.ZERO);
                    }
                }

                DebtPayoffPlan avalanchePlan = payoffCalculatorService.calculateAvalancheMethod(
                        debtEntities, monthlyPayment
                );

                DebtPayoffPlan snowballPlan = payoffCalculatorService.calculateSnowballMethod(
                        debtEntities, monthlyPayment
                );

                resultsLayout.removeAll();
                resultsLayout.add(
                        createPayoffPlanCard(avalanchePlan),
                        createPayoffPlanCard(snowballPlan)
                );

            } catch (Exception e) {
                showErrorNotification("Error calculating payoff: " + e.getMessage());
            }
        };

        calculateButton.addClickListener(event -> runCalculation.run());

        HorizontalLayout inputLayout = new HorizontalLayout(monthlyPaymentField, calculateButton);
        inputLayout.setAlignItems(FlexComponent.Alignment.END);

        content.add(inputLayout, resultsLayout);
        calculatorDialog.add(content);

        // Auto-calculate on open
        calculatorDialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                runCalculation.run();
            }
        });

        calculatorDialog.open();
    }

    private VerticalLayout createPayoffPlanCard(final DebtPayoffPlan plan) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(true);
        card.setPadding(true);
        card.getStyle()
                .set("background-color", "var(--finance-card-bg)")
                .set("border", "1px solid var(--finance-border)")
                .set("border-radius", "0.5rem")
                .set("padding", "1.5rem");

        H3 strategyName = new H3(plan.getStrategyName());
        strategyName.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "var(--finance-text-primary)");

        HorizontalLayout summary = new HorizontalLayout();
        summary.setWidthFull();
        summary.setSpacing(true);

        summary.add(
                createMetric("Payoff Time", plan.getTotalMonthsToPayoff() + " months"),
                createMetric("Total Interest", "£" + plan.getTotalInterestPaid()),
                createMetric("Total Paid", "£" + plan.getTotalAmountPaid())
        );

        Grid<DebtPayoffProjection> grid = new Grid<>(DebtPayoffProjection.class, false);
        grid.addColumn(DebtPayoffProjection::getDebtName).setHeader("Debt").setAutoWidth(true);
        grid.addColumn(proj -> "£" + proj.getMonthlyPayment()).setHeader("Monthly Payment").setAutoWidth(true);
        grid.addColumn(proj -> proj.getMonthsToPayoff() + " months").setHeader("Payoff Time").setAutoWidth(true);
        grid.addColumn(proj -> "£" + proj.getTotalInterestPaid()).setHeader("Interest").setAutoWidth(true);

        grid.setItems(plan.getDebtProjections());
        grid.setAllRowsVisible(true);

        card.add(strategyName, summary, grid);
        return card;
    }

    private VerticalLayout createMetric(final String label, final String value) {
        VerticalLayout metric = new VerticalLayout();
        metric.setSpacing(false);
        metric.setPadding(false);
        metric.getStyle().set("flex", "1");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.75rem")
                .set("color", "var(--finance-text-secondary)")
                .set("text-transform", "uppercase")
                .set("font-weight", "600");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "1.25rem")
                .set("font-weight", "700")
                .set("color", "var(--finance-text-primary)");

        metric.add(labelSpan, valueSpan);
        return metric;
    }

    private void refreshGrid() {
        debtGrid.setItems(getAllDebts());
        removeAll();
        add(createToolbar(), createSummaryCards(), debtGrid);
    }

    private List<DebtDTO> getAllDebts() {
        User currentUser = getCurrentUser();
        return debtService.findAllAsDto().stream()
                .filter(debt -> debt.getUserId().equals(currentUser.getUserId()))
                .collect(Collectors.toList());
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
