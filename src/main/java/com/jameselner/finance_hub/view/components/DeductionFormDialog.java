package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.DeductionType;
import com.jameselner.finance_hub.dto.IncomeDeductionDTO;
import com.jameselner.finance_hub.service.IncomeDeductionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import java.util.Objects;
import java.util.function.Consumer;

public class DeductionFormDialog extends Dialog {

    private final IncomeDeductionService incomeDeductionService;
    private final Binder<IncomeDeductionDTO> binder = new BeanValidationBinder<>(IncomeDeductionDTO.class);

    private Long incomeSourceId;
    private IncomeDeductionDTO currentDeduction;

    private ComboBox<DeductionType> deductionTypeCombo;
    private Checkbox isPercentageCheckbox;
    private BigDecimalField amountField;
    private BigDecimalField percentageField;
    private TextArea descriptionField;
    private Checkbox isActiveCheckbox;

    @Setter
    private Consumer<IncomeDeductionDTO> saveListener;

    public DeductionFormDialog(final IncomeDeductionService incomeDeductionService) {
        this.incomeDeductionService = incomeDeductionService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("min(500px, 95vw)");
        setMaxHeight("90vh");

        createFormFields();
        setupBinder();
        setupFieldListeners();
    }

    public void open(final Long incomeSourceId, final IncomeDeductionDTO deduction) {
        this.incomeSourceId = incomeSourceId;
        this.currentDeduction = deduction;

        removeAll();

        H3 title = new H3(deduction != null && deduction.getDeductionId() != null ?
                "Edit Deduction" : "Add Deduction");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (deduction != null) {
            binder.readBean(deduction);
            updateFieldVisibility();
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        deductionTypeCombo = new ComboBox<>("Deduction Type");
        deductionTypeCombo.setItems(DeductionType.values());
        deductionTypeCombo.setItemLabelGenerator(DeductionType::getDisplayName);
        deductionTypeCombo.setRequiredIndicatorVisible(true);

        isPercentageCheckbox = new Checkbox("Use Percentage");
        isPercentageCheckbox.setHelperText("Check to use percentage of gross salary");

        amountField = new BigDecimalField("Fixed Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setHelperText("Fixed deduction amount");

        percentageField = new BigDecimalField("Percentage");
        percentageField.setSuffixComponent(new Span("%"));
        percentageField.setHelperText("Percentage of gross salary");
        percentageField.setEnabled(false);

        descriptionField = new TextArea("Description");
        descriptionField.setHelperText("Optional notes about this deduction");
        descriptionField.setMaxLength(500);

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive deductions won't be applied");
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        formLayout.add(
                deductionTypeCombo,
                isPercentageCheckbox,
                amountField,
                percentageField,
                descriptionField,
                isActiveCheckbox
        );

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveDeduction());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(deductionTypeCombo)
                .withValidator(Objects::nonNull, "Deduction type is required")
                .bind(IncomeDeductionDTO::getDeductionType, IncomeDeductionDTO::setDeductionType);

        binder.forField(isPercentageCheckbox)
                .bind(IncomeDeductionDTO::getIsPercentage, IncomeDeductionDTO::setIsPercentage);

        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must not be negative",
                        BigDecimal.ZERO,
                        new BigDecimal("999999999.99")))
                .bind(IncomeDeductionDTO::getAmount, IncomeDeductionDTO::setAmount);

        binder.forField(percentageField)
                .withValidator(new BigDecimalRangeValidator(
                        "Percentage must be between 0 and 100",
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(100)))
                .bind(IncomeDeductionDTO::getPercentageValue, IncomeDeductionDTO::setPercentageValue);

        binder.forField(descriptionField)
                .bind(IncomeDeductionDTO::getDescription, IncomeDeductionDTO::setDescription);

        binder.forField(isActiveCheckbox)
                .bind(IncomeDeductionDTO::getIsActive, IncomeDeductionDTO::setIsActive);
    }

    private void setupFieldListeners() {
        isPercentageCheckbox.addValueChangeListener(event -> updateFieldVisibility());
    }

    private void updateFieldVisibility() {
        boolean usePercentage = isPercentageCheckbox.getValue();
        amountField.setEnabled(!usePercentage);
        amountField.setRequiredIndicatorVisible(!usePercentage);
        percentageField.setEnabled(usePercentage);
        percentageField.setRequiredIndicatorVisible(usePercentage);

        if (usePercentage) {
            amountField.clear();
        } else {
            percentageField.clear();
        }
    }

    private void saveDeduction() {
        try {
            IncomeDeductionDTO dto = currentDeduction != null ? currentDeduction : new IncomeDeductionDTO();
            dto.setIncomeSourceId(incomeSourceId);

            binder.writeBean(dto);

            // Auto-set deduction name from deduction type
            if (dto.getDeductionType() != null) {
                dto.setDeductionName(dto.getDeductionType().getDisplayName());
            }

            // Ensure the unused field is null to satisfy database constraints
            if (dto.getIsPercentage()) {
                dto.setAmount(null);
            } else {
                dto.setPercentageValue(null);
            }

            IncomeDeductionDTO savedDto;

            // If incomeSourceId is null, this is a new income source not yet saved
            // Just return the DTO without saving to database
            if (incomeSourceId == null) {
                savedDto = dto;
                showSuccessNotification("Deduction added (will be saved with income source)");
            } else {
                // Normal flow: save to database
                if (dto.getDeductionId() == null) {
                    savedDto = incomeDeductionService.createDeductionFromDto(dto);
                    showSuccessNotification("Deduction added successfully");
                } else {
                    savedDto = incomeDeductionService.updateDeductionFromDto(dto);
                    showSuccessNotification("Deduction updated successfully");
                }
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving deduction: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentDeduction = null;
        IncomeDeductionDTO emptyDto = new IncomeDeductionDTO();
        emptyDto.setIsPercentage(false);
        emptyDto.setIsActive(true);
        binder.readBean(emptyDto);
        updateFieldVisibility();
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
