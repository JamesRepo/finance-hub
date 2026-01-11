package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.DebtPaymentDTO;
import com.jameselner.finance_hub.service.DebtPaymentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DebtPaymentDialog extends Dialog {

    private final DebtPaymentService debtPaymentService;

    private BigDecimalField paymentAmountField;
    private DatePicker paymentDateField;
    private Button saveButton;
    private Button cancelButton;

    private DebtDTO currentDebt;
    private Runnable onSuccessCallback;

    public DebtPaymentDialog(final DebtPaymentService debtPaymentService) {
        this.debtPaymentService = debtPaymentService;

        setHeaderTitle("Record Payment");
        setWidth("min(500px, 95vw)");
        setMaxHeight("90vh");

        createFormFields();
        createButtons();

        add(createContent());
    }

    private void createFormFields() {
        paymentAmountField = new BigDecimalField("Payment Amount");
        paymentAmountField.setPrefixComponent(new Span("£"));
        paymentAmountField.setRequiredIndicatorVisible(true);
        paymentAmountField.setHelperText("Enter the payment amount");
        paymentAmountField.setWidth("100%");

        paymentDateField = new DatePicker("Payment Date");
        paymentDateField.setRequiredIndicatorVisible(true);
        paymentDateField.setValue(LocalDate.now());
        paymentDateField.setHelperText("Date of payment");
        paymentDateField.setWidth("100%");
    }

    private void createButtons() {
        saveButton = new Button("Record Payment");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> recordPayment());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.add(paymentAmountField, paymentDateField);

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        content.add(createDebtInfo(), formLayout, buttonLayout);
        return content;
    }

    private VerticalLayout createDebtInfo() {
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setSpacing(false);
        infoLayout.setPadding(false);
        infoLayout.getStyle()
                .set("background-color", "var(--finance-bg)")
                .set("border-radius", "0.5rem")
                .set("padding", "1rem")
                .set("margin-bottom", "1rem");

        if (currentDebt != null) {
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
            balanceInfo.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            VerticalLayout currentBalanceLayout = new VerticalLayout();
            currentBalanceLayout.setSpacing(false);
            currentBalanceLayout.setPadding(false);

            Span balanceLabel = new Span("Current Balance");
            balanceLabel.getStyle()
                    .set("font-size", "0.75rem")
                    .set("color", "var(--finance-text-secondary)")
                    .set("text-transform", "uppercase")
                    .set("font-weight", "600");

            Span balanceValue = new Span("£" + currentDebt.getCurrentBalance());
            balanceValue.getStyle()
                    .set("font-size", "1.5rem")
                    .set("font-weight", "700")
                    .set("color", "var(--finance-text-primary)");

            currentBalanceLayout.add(balanceLabel, balanceValue);

            VerticalLayout interestRateLayout = new VerticalLayout();
            interestRateLayout.setSpacing(false);
            interestRateLayout.setPadding(false);

            Span rateLabel = new Span("Interest Rate");
            rateLabel.getStyle()
                    .set("font-size", "0.75rem")
                    .set("color", "var(--finance-text-secondary)")
                    .set("text-transform", "uppercase")
                    .set("font-weight", "600");

            Span rateValue = new Span(currentDebt.getInterestRate() + "%");
            rateValue.getStyle()
                    .set("font-size", "1.5rem")
                    .set("font-weight", "700")
                    .set("color", "var(--finance-text-primary)");

            interestRateLayout.add(rateLabel, rateValue);

            balanceInfo.add(currentBalanceLayout, interestRateLayout);

            infoLayout.add(debtName, debtType, balanceInfo);
        }

        return infoLayout;
    }

    private void recordPayment() {
        try {
            if (currentDebt == null) {
                showErrorNotification("No debt selected");
                return;
            }

            if (paymentAmountField.getValue() == null || paymentAmountField.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                showErrorNotification("Payment amount must be greater than zero");
                return;
            }

            if (paymentDateField.getValue() == null) {
                showErrorNotification("Payment date is required");
                return;
            }

            BigDecimal paymentAmount = paymentAmountField.getValue();
            LocalDate paymentDate = paymentDateField.getValue();

            DebtPaymentDTO result = debtPaymentService.recordPaymentWithInterestCalculation(
                    currentDebt.getDebtId(),
                    paymentAmount,
                    paymentDate
            );

            showPaymentBreakdownNotification(result);
            close();

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }

        } catch (Exception e) {
            showErrorNotification("Error recording payment: " + e.getMessage());
        }
    }

    private void showPaymentBreakdownNotification(final DebtPaymentDTO payment) {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        Span title = new Span("Payment recorded successfully");
        title.getStyle()
                .set("font-weight", "600")
                .set("display", "block")
                .set("margin-bottom", "0.5rem");

        Span principalLine = new Span(String.format("Principal paid: £%.2f", payment.getPrincipalPaid()));
        principalLine.getStyle()
                .set("display", "block")
                .set("color", "var(--finance-secondary)");

        Span interestLine = new Span(String.format("Interest paid: £%.2f", payment.getInterestPaid()));
        interestLine.getStyle()
                .set("display", "block")
                .set("color", "var(--finance-warning)");

        content.add(title, principalLine, interestLine);

        Notification notification = new Notification();
        notification.add(content);
        notification.setDuration(5000);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public void openForDebt(final DebtDTO debt, final Runnable onSuccess) {
        this.currentDebt = debt;
        this.onSuccessCallback = onSuccess;

        paymentAmountField.clear();
        paymentDateField.setValue(LocalDate.now());

        if (debt != null && debt.getMinimumPayment() != null) {
            paymentAmountField.setValue(debt.getMinimumPayment());
        }

        removeAll();
        add(createContent());

        open();
    }

    private void showErrorNotification(final String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
