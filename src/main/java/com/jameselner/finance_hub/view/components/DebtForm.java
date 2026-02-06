package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.service.CurrentUserService;
import com.jameselner.finance_hub.service.DebtService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

public class DebtForm extends VerticalLayout {

    private final DebtService debtService;
    private final CurrentUserService currentUserService;

    private final Binder<DebtDTO> binder = new BeanValidationBinder<>(DebtDTO.class);
    private DebtDTO currentDebt;

    private TextField nameField;
    private ComboBox<DebtType> debtTypeField;
    private BigDecimalField principalAmountField;
    private BigDecimalField currentBalanceField;
    private BigDecimalField interestRateField;
    private BigDecimalField minimumPaymentField;
    private DatePicker startDateField;
    private DatePicker targetPayoffDateField;
    private TextArea notesField;

    private Button saveButton;
    private Button cancelButton;

    @Setter
    private Consumer<DebtDTO> saveListener;
    @Setter
    private Runnable cancelListener;

    public DebtForm(
            final DebtService debtService,
            final CurrentUserService currentUserService
    ) {
        this.debtService = debtService;
        this.currentUserService = currentUserService;

        addClassName("debt-form");
        setSpacing(true);
        setPadding(true);

        createFormFields();
        createButtons();

        add(createFormLayout(), createButtonLayout());

        setupBinder();
    }

    private void createFormFields() {
        nameField = new TextField("Debt Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("e.g., Chase Credit Card, Student Loan");
        nameField.setPlaceholder("Enter debt name");

        debtTypeField = new ComboBox<>("Debt Type");
        debtTypeField.setItems(DebtType.values());
        debtTypeField.setItemLabelGenerator(DebtType::getDisplayName);
        debtTypeField.setRequiredIndicatorVisible(true);
        debtTypeField.setHelperText("Select the type of debt");
        debtTypeField.setValue(DebtType.CREDIT_CARD);

        principalAmountField = new BigDecimalField("Original Amount");
        principalAmountField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        principalAmountField.setHelperText("Original loan/debt amount (optional)");

        currentBalanceField = new BigDecimalField("Current Balance");
        currentBalanceField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        currentBalanceField.setRequiredIndicatorVisible(true);
        currentBalanceField.setHelperText("Current outstanding balance");

        interestRateField = new BigDecimalField("Interest Rate (APR)");
        interestRateField.setSuffixComponent(new com.vaadin.flow.component.html.Span("%"));
        interestRateField.setRequiredIndicatorVisible(true);
        interestRateField.setHelperText("Annual percentage rate");

        minimumPaymentField = new BigDecimalField("Minimum Payment");
        minimumPaymentField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        minimumPaymentField.setHelperText("Monthly minimum payment (optional)");

        startDateField = new DatePicker("Start Date");
        startDateField.setHelperText("When you took out this debt (optional)");

        targetPayoffDateField = new DatePicker("Target Payoff Date");
        targetPayoffDateField.setHelperText("Optional target date to be debt-free");

        notesField = new TextArea("Notes");
        notesField.setPlaceholder("Add any notes about this debt...");
        notesField.setHelperText("Optional notes (e.g., account number, lender contact info)");
        notesField.setMaxLength(1000);
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(nameField);
        formLayout.setColspan(nameField, 2);

        formLayout.add(
                debtTypeField,
                principalAmountField,
                currentBalanceField,
                interestRateField,
                minimumPaymentField,
                startDateField,
                targetPayoffDateField
        );

        formLayout.add(notesField);
        formLayout.setColspan(notesField, 2);

        return formLayout;
    }

    private void createButtons() {
        saveButton = new Button("Save Debt");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveDebt());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> cancel());
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(nameField)
                .withValidator(Objects::nonNull, "Debt name is required")
                .withValidator(name -> !name.trim().isEmpty(), "Debt name cannot be empty")
                .bind(DebtDTO::getDebtName, DebtDTO::setDebtName);

        binder.forField(debtTypeField)
                .withValidator(Objects::nonNull, "Debt type is required")
                .bind(DebtDTO::getDebtType, DebtDTO::setDebtType);

        binder.forField(principalAmountField)
                .withValidator(value -> value == null || value.compareTo(BigDecimal.ZERO) > 0,
                        "Amount must be greater than zero if provided")
                .bind(DebtDTO::getPrincipalAmount, DebtDTO::setPrincipalAmount);

        binder.forField(currentBalanceField)
                .withValidator(new BigDecimalRangeValidator(
                        "Balance must not be negative",
                        BigDecimal.ZERO,
                        new BigDecimal("999999999.99")))
                .bind(DebtDTO::getCurrentBalance, DebtDTO::setCurrentBalance);

        binder.forField(interestRateField)
                .withValidator(new BigDecimalRangeValidator(
                        "Interest rate must be between 0 and 100",
                        BigDecimal.ZERO,
                        new BigDecimal("100")))
                .bind(DebtDTO::getInterestRate, DebtDTO::setInterestRate);

        binder.forField(minimumPaymentField)
                .withValidator(value -> value == null || value.compareTo(BigDecimal.ZERO) >= 0,
                        "Minimum payment must not be negative")
                .bind(DebtDTO::getMinimumPayment, DebtDTO::setMinimumPayment);

        binder.forField(startDateField)
                .bind(DebtDTO::getStartDate, DebtDTO::setStartDate);

        binder.forField(targetPayoffDateField)
                .bind(DebtDTO::getTargetPayoffDate, DebtDTO::setTargetPayoffDate);

        binder.forField(notesField)
                .bind(DebtDTO::getNotes, DebtDTO::setNotes);
    }

    private void saveDebt() {
        try {
            DebtDTO debt = currentDebt != null ? currentDebt : new DebtDTO();

            User currentUser = getCurrentUser();
            debt.setUserId(currentUser.getUserId());

            binder.writeBean(debt);

            DebtDTO savedDebt;
            if (debt.getDebtId() == null) {
                savedDebt = debtService.createDebtFromDto(debt);
                showSuccessNotification("Debt created successfully");
            } else {
                savedDebt = debtService.updateDebtFromDto(debt);
                showSuccessNotification("Debt updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDebt);
            }

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving debt: " + e.getMessage());
        }
    }

    private void cancel() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    public void setDebt(final DebtDTO debt) {
        this.currentDebt = debt;
        binder.readBean(debt);
    }

    public void clearForm() {
        this.currentDebt = null;
        binder.readBean(new DebtDTO());
        debtTypeField.setValue(DebtType.CREDIT_CARD);
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
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
