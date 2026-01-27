package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.DebtPaymentDTO;
import com.jameselner.finance_hub.service.DebtPaymentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DebtPaymentsViewDialog extends Dialog {

    private final DebtPaymentService debtPaymentService;

    private Grid<DebtPaymentDTO> paymentsGrid;
    private DebtDTO currentDebt;
    private Runnable onChangeCallback;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public DebtPaymentsViewDialog(final DebtPaymentService debtPaymentService) {
        this.debtPaymentService = debtPaymentService;

        setHeaderTitle("Payment History");
        setWidth("min(800px, 95vw)");
        setMaxHeight("90vh");

        createGrid();
    }

    private void createGrid() {
        paymentsGrid = new Grid<>(DebtPaymentDTO.class, false);
        paymentsGrid.setWidthFull();
        paymentsGrid.setAllRowsVisible(true);

        paymentsGrid.addColumn(payment -> payment.getPaymentDate().format(DATE_FORMATTER))
                .setHeader("Date")
                .setAutoWidth(true)
                .setSortable(true);

        paymentsGrid.addColumn(payment -> "£" + payment.getPaymentAmount())
                .setHeader("Amount")
                .setAutoWidth(true);

        paymentsGrid.addColumn(payment -> "£" + payment.getPrincipalPaid())
                .setHeader("Principal Paid")
                .setAutoWidth(true);

        paymentsGrid.addColumn(payment -> "£" + payment.getInterestPaid())
                .setHeader("Interest Paid")
                .setAutoWidth(true);

        paymentsGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final DebtPaymentDTO payment) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.getElement().setAttribute("aria-label", "Edit payment");
        editButton.addClickListener(event -> openEditDialog(payment));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getElement().setAttribute("aria-label", "Delete payment");
        deleteButton.addClickListener(event -> showDeleteConfirmation(payment));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openEditDialog(final DebtPaymentDTO payment) {
        Dialog editDialog = new Dialog();
        editDialog.setHeaderTitle("Edit Payment");
        editDialog.setWidth("min(400px, 95vw)");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        BigDecimalField amountField = new BigDecimalField("Total Payment Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setValue(payment.getPaymentAmount());
        amountField.setRequiredIndicatorVisible(true);
        amountField.setWidthFull();

        BigDecimalField interestField = new BigDecimalField("Interest Paid");
        interestField.setPrefixComponent(new Span("£"));
        interestField.setValue(payment.getInterestPaid());
        interestField.setRequiredIndicatorVisible(true);
        interestField.setHelperText("Amount that went to interest");
        interestField.setWidthFull();

        DatePicker dateField = new DatePicker("Payment Date");
        dateField.setValue(payment.getPaymentDate());
        dateField.setRequiredIndicatorVisible(true);
        dateField.setWidthFull();

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            try {
                BigDecimal amount = amountField.getValue();
                BigDecimal interest = interestField.getValue();
                LocalDate date = dateField.getValue();

                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorNotification("Amount must be greater than zero");
                    return;
                }
                if (interest == null || interest.compareTo(BigDecimal.ZERO) < 0) {
                    showErrorNotification("Interest must not be negative");
                    return;
                }
                if (interest.compareTo(amount) > 0) {
                    showErrorNotification("Interest cannot exceed payment amount");
                    return;
                }
                if (date == null) {
                    showErrorNotification("Date is required");
                    return;
                }

                debtPaymentService.updatePayment(payment.getPaymentId(), amount, interest, date);
                editDialog.close();
                refreshGrid();
                showSuccessNotification("Payment updated successfully");

                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }
            } catch (Exception e) {
                showErrorNotification("Error updating payment: " + e.getMessage());
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> editDialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        content.add(amountField, interestField, dateField, buttonLayout);
        editDialog.add(content);
        editDialog.open();
    }

    private void showDeleteConfirmation(final DebtPaymentDTO payment) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Payment");
        dialog.setText(String.format(
                "Are you sure you want to delete this payment of £%s from %s? " +
                        "The debt balance will be restored by £%s (the principal portion).",
                payment.getPaymentAmount(),
                payment.getPaymentDate().format(DATE_FORMATTER),
                payment.getPrincipalPaid()
        ));

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                debtPaymentService.deleteById(payment.getPaymentId());
                refreshGrid();
                showSuccessNotification("Payment deleted successfully");

                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }
            } catch (Exception ex) {
                showErrorNotification("Error deleting payment: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setSizeFull();

        if (currentDebt != null) {
            content.add(createDebtHeader());
        }

        content.add(paymentsGrid);

        Button closeButton = new Button("Close");
        closeButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(closeButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        content.add(buttonLayout);
        return content;
    }

    private VerticalLayout createDebtHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.getStyle()
                .set("background-color", "var(--finance-bg)")
                .set("border-radius", "0.5rem")
                .set("padding", "1rem")
                .set("margin-bottom", "1rem");

        H3 debtName = new H3(currentDebt.getDebtName());
        debtName.getStyle()
                .set("margin", "0 0 0.5rem 0")
                .set("font-size", "1.125rem");

        Span debtType = new Span(currentDebt.getDebtType().toString());
        debtType.getStyle()
                .set("font-size", "0.875rem")
                .set("color", "var(--finance-text-secondary)")
                .set("display", "block")
                .set("margin-bottom", "0.75rem");

        HorizontalLayout balanceInfo = new HorizontalLayout();
        balanceInfo.setSpacing(true);
        balanceInfo.setWidthFull();

        balanceInfo.add(
                createMetric("Current Balance", "£" + currentDebt.getCurrentBalance()),
                createMetric("Interest Rate", currentDebt.getInterestRate() + "%")
        );

        header.add(debtName, debtType, balanceInfo);
        return header;
    }

    private VerticalLayout createMetric(final String label, final String value) {
        VerticalLayout metric = new VerticalLayout();
        metric.setSpacing(false);
        metric.setPadding(false);

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
        if (currentDebt != null) {
            List<DebtPaymentDTO> payments = debtPaymentService.findByDebtId(currentDebt.getDebtId());
            paymentsGrid.setItems(payments);
        }
    }

    public void openForDebt(final DebtDTO debt, final Runnable onChange) {
        this.currentDebt = debt;
        this.onChangeCallback = onChange;

        setHeaderTitle("Payment History - " + debt.getDebtName());

        removeAll();
        add(createContent());
        refreshGrid();

        open();
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
