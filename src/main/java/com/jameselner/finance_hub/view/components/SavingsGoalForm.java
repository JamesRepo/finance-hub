package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.Priority;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.SavingsGoalService;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class SavingsGoalForm extends VerticalLayout {

    private final SavingsGoalService savingsGoalService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private final Binder<SavingsGoalDTO> binder = new BeanValidationBinder<>(SavingsGoalDTO.class);
    private SavingsGoalDTO currentGoal;

    private TextField nameField;
    private BigDecimalField targetAmountField;
    private BigDecimalField currentAmountField;
    private DatePicker targetDateField;
    private ComboBox<Priority> priorityField;

    private Button saveButton;
    private Button cancelButton;

    @Setter
    private Consumer<SavingsGoalDTO> saveListener;
    @Setter
    private Runnable cancelListener;

    public SavingsGoalForm(
            final SavingsGoalService savingsGoalService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.savingsGoalService = savingsGoalService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        addClassName("savings-goal-form");
        setSpacing(true);
        setPadding(true);

        createFormFields();
        createButtons();

        add(createFormLayout(), createButtonLayout());

        setupBinder();
    }

    private void createFormFields() {
        nameField = new TextField("Goal Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("e.g., Emergency Fund, Vacation, New Car");
        nameField.setPlaceholder("Enter goal name");

        targetAmountField = new BigDecimalField("Target Amount");
        targetAmountField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        targetAmountField.setRequiredIndicatorVisible(true);
        targetAmountField.setHelperText("How much do you want to save?");

        currentAmountField = new BigDecimalField("Current Amount");
        currentAmountField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        currentAmountField.setHelperText("How much have you saved so far?");
        currentAmountField.setValue(BigDecimal.ZERO);

        targetDateField = new DatePicker("Target Date");
        targetDateField.setHelperText("When do you want to reach this goal?");
        targetDateField.setMin(LocalDate.now());

        priorityField = new ComboBox<>("Priority");
        priorityField.setItems(Priority.values());
        priorityField.setHelperText("How important is this goal?");
        priorityField.setValue(Priority.MEDIUM);
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
                targetAmountField,
                currentAmountField,
                targetDateField,
                priorityField
        );

        return formLayout;
    }

    private void createButtons() {
        saveButton = new Button("Save Goal");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveGoal());

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
                .withValidator(Objects::nonNull, "Goal name is required")
                .withValidator(name -> !name.trim().isEmpty(), "Goal name cannot be empty")
                .bind(SavingsGoalDTO::getGoalName, SavingsGoalDTO::setGoalName);

        binder.forField(targetAmountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(SavingsGoalDTO::getTargetAmount, SavingsGoalDTO::setTargetAmount);

        binder.forField(currentAmountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must not be negative",
                        BigDecimal.ZERO,
                        new BigDecimal("999999999.99")))
                .bind(SavingsGoalDTO::getCurrentAmount, SavingsGoalDTO::setCurrentAmount);

        binder.forField(targetDateField)
                .bind(SavingsGoalDTO::getTargetDate, SavingsGoalDTO::setTargetDate);

        binder.forField(priorityField)
                .bind(SavingsGoalDTO::getPriority, SavingsGoalDTO::setPriority);
    }

    private void saveGoal() {
        try {
            SavingsGoalDTO goal = currentGoal != null ? currentGoal : new SavingsGoalDTO();

            User currentUser = getCurrentUser();
            goal.setUserId(currentUser.getUserId());

            binder.writeBean(goal);

            SavingsGoalDTO savedGoal;
            if (goal.getGoalId() == null) {
                savedGoal = savingsGoalService.createSavingsGoalFromDto(goal);
                showSuccessNotification("Savings goal created successfully");
            } else {
                savedGoal = savingsGoalService.updateSavingsGoalFromDto(goal);
                showSuccessNotification("Savings goal updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedGoal);
            }

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving goal: " + e.getMessage());
        }
    }

    private void cancel() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    public void setGoal(final SavingsGoalDTO goal) {
        this.currentGoal = goal;
        binder.readBean(goal);
    }

    public void clearForm() {
        this.currentGoal = null;
        SavingsGoalDTO emptyGoal = new SavingsGoalDTO();
        emptyGoal.setCurrentAmount(BigDecimal.ZERO);
        binder.readBean(emptyGoal);
        priorityField.setValue(Priority.MEDIUM);
        currentAmountField.setValue(BigDecimal.ZERO);
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
