package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.dto.SavingsGoalContributionDTO;
import com.jameselner.finance_hub.dto.SavingsGoalProgressDTO;
import com.jameselner.finance_hub.service.SavingsGoalContributionService;
import com.jameselner.finance_hub.service.SavingsGoalService;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class SavingsGoalContributionDialog extends Dialog {

    private final SavingsGoalContributionService contributionService;
    private final SavingsGoalService savingsGoalService;
    private final Binder<SavingsGoalContributionDTO> binder = new BeanValidationBinder<>(SavingsGoalContributionDTO.class);

    private Long goalId;
    private BigDecimalField amountField;
    private DatePicker contributionDateField;
    private TextArea noteField;

    @Setter
    private Consumer<SavingsGoalContributionDTO> saveListener;

    public SavingsGoalContributionDialog(
            final SavingsGoalContributionService contributionService,
            final SavingsGoalService savingsGoalService
    ) {
        this.contributionService = contributionService;
        this.savingsGoalService = savingsGoalService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("500px");
    }

    public void open(final Long goalId, final String goalName) {
        this.goalId = goalId;

        removeAll();

        H3 title = new H3("Record Contribution");
        title.getStyle().set("margin", "0");

        SavingsGoalProgressDTO progress = savingsGoalService.calculateProgress(goalId);

        VerticalLayout contextLayout = createContextLayout(goalName, progress);
        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, contextLayout, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        setupBinder();
        clearForm();

        open();
    }

    private VerticalLayout createContextLayout(final String goalName, final SavingsGoalProgressDTO progress) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Span goalNameSpan = new Span("Goal: " + goalName);
        goalNameSpan.getStyle().set("font-weight", "bold");

        Span currentAmountSpan = new Span(String.format("Current: £%.2f", progress.getCurrentAmount()));
        Span targetAmountSpan = new Span(String.format("Target: £%.2f", progress.getTargetAmount()));
        Span progressSpan = new Span(String.format("Progress: %.1f%%", progress.getPercentageComplete()));

        layout.add(goalNameSpan, currentAmountSpan, targetAmountSpan, progressSpan);

        return layout;
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        amountField = new BigDecimalField("Contribution Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Enter the amount you're contributing");
        amountField.setPlaceholder("0.00");

        contributionDateField = new DatePicker("Contribution Date");
        contributionDateField.setRequiredIndicatorVisible(true);
        contributionDateField.setValue(LocalDate.now());
        contributionDateField.setMax(LocalDate.now());

        noteField = new TextArea("Note");
        noteField.setHelperText("Optional note about this contribution");
        noteField.setPlaceholder("e.g., Monthly savings deposit");
        noteField.setMaxLength(500);

        formLayout.add(amountField, contributionDateField, noteField);

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Record Contribution");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveContribution());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(SavingsGoalContributionDTO::getAmount, SavingsGoalContributionDTO::setAmount);

        binder.forField(contributionDateField)
                .withValidator(Objects::nonNull, "Contribution date is required")
                .bind(SavingsGoalContributionDTO::getContributionDate, SavingsGoalContributionDTO::setContributionDate);

        binder.forField(noteField)
                .bind(SavingsGoalContributionDTO::getNote, SavingsGoalContributionDTO::setNote);
    }

    private void saveContribution() {
        try {
            SavingsGoalContributionDTO contributionDto = new SavingsGoalContributionDTO();
            contributionDto.setGoalId(goalId);

            binder.writeBean(contributionDto);

            SavingsGoalContributionDTO savedContribution = contributionService.recordContribution(
                    contributionDto.getGoalId(),
                    contributionDto.getAmount(),
                    contributionDto.getContributionDate(),
                    contributionDto.getNote()
            );

            showSuccessNotification(String.format("Contribution of £%.2f recorded successfully",
                    savedContribution.getAmount()));

            if (saveListener != null) {
                saveListener.accept(savedContribution);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error recording contribution: " + e.getMessage());
        }
    }

    private void clearForm() {
        SavingsGoalContributionDTO emptyDto = new SavingsGoalContributionDTO();
        emptyDto.setContributionDate(LocalDate.now());
        binder.readBean(emptyDto);
        amountField.focus();
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
