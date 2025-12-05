package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.Frequency;
import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.service.HousingExpenseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class HousingExpenseFormDialog extends Dialog {

    private final HousingExpenseService housingExpenseService;
    private final Binder<HousingExpenseDTO> binder = new BeanValidationBinder<>(HousingExpenseDTO.class);

    private Long userId;
    private HousingExpenseDTO currentExpense;

    private ComboBox<HousingExpenseType> expenseTypeCombo;
    private BigDecimalField amountField;
    private ComboBox<Frequency> frequencyCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Checkbox isActiveCheckbox;

    private Span monthlyEquivalentDisplay;
    private Span annualEquivalentDisplay;

    @Setter
    private Consumer<HousingExpenseDTO> saveListener;

    public HousingExpenseFormDialog(final HousingExpenseService housingExpenseService) {
        this.housingExpenseService = housingExpenseService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("600px");

        createFormFields();
        setupBinder();
        setupFieldListeners();
    }

    public void open(final Long userId, final HousingExpenseDTO expense) {
        this.userId = userId;
        this.currentExpense = expense;

        removeAll();

        H3 title = new H3(expense != null && expense.getExpenseId() != null ?
                "Edit Housing Expense" : "Add Housing Expense");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        VerticalLayout equivalentsSection = createEquivalentsSection();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, equivalentsSection, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (expense != null) {
            binder.readBean(expense);
            updateEquivalents();
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        expenseTypeCombo = new ComboBox<>("Expense Type");
        expenseTypeCombo.setItems(HousingExpenseType.values());
        expenseTypeCombo.setItemLabelGenerator(HousingExpenseType::getDisplayName);
        expenseTypeCombo.setRequiredIndicatorVisible(true);
        expenseTypeCombo.setHelperText("Select the type of housing expense");

        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Enter the expense amount");

        frequencyCombo = new ComboBox<>("Frequency");
        frequencyCombo.setItems(Frequency.values());
        frequencyCombo.setItemLabelGenerator(Frequency::getDisplayName);
        frequencyCombo.setRequiredIndicatorVisible(true);
        frequencyCombo.setHelperText("How often is this expense paid?");

        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setRequiredIndicatorVisible(true);
        startDatePicker.setValue(LocalDate.now());
        startDatePicker.setHelperText("When does/did this expense start?");

        endDatePicker = new DatePicker("End Date");
        endDatePicker.setHelperText("Optional: When does this expense end?");

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive expenses won't be included in calculations");
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(expenseTypeCombo);
        formLayout.setColspan(expenseTypeCombo, 2);

        formLayout.add(
                amountField,
                frequencyCombo,
                startDatePicker,
                endDatePicker,
                isActiveCheckbox
        );

        return formLayout;
    }

    private VerticalLayout createEquivalentsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "0.5rem");

        Span sectionTitle = new Span("Cost Breakdown");
        sectionTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout monthlyLayout = new HorizontalLayout();
        monthlyLayout.setSpacing(true);
        monthlyLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        Span monthlyLabel = new Span("Monthly:");
        monthlyLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        monthlyEquivalentDisplay = new Span("£0.00");
        monthlyEquivalentDisplay.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");
        monthlyLayout.add(monthlyLabel, monthlyEquivalentDisplay);

        HorizontalLayout annualLayout = new HorizontalLayout();
        annualLayout.setSpacing(true);
        annualLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        Span annualLabel = new Span("Annual:");
        annualLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        annualEquivalentDisplay = new Span("£0.00");
        annualEquivalentDisplay.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");
        annualLayout.add(annualLabel, annualEquivalentDisplay);

        section.add(sectionTitle, monthlyLayout, annualLayout);

        return section;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveExpense());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(expenseTypeCombo)
                .withValidator(Objects::nonNull, "Expense type is required")
                .bind(HousingExpenseDTO::getExpenseType, HousingExpenseDTO::setExpenseType);

        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(HousingExpenseDTO::getAmount, HousingExpenseDTO::setAmount);

        binder.forField(frequencyCombo)
                .withValidator(Objects::nonNull, "Frequency is required")
                .bind(HousingExpenseDTO::getFrequency, HousingExpenseDTO::setFrequency);

        binder.forField(startDatePicker)
                .withValidator(Objects::nonNull, "Start date is required")
                .bind(HousingExpenseDTO::getStartDate, HousingExpenseDTO::setStartDate);

        binder.forField(endDatePicker)
                .bind(HousingExpenseDTO::getEndDate, HousingExpenseDTO::setEndDate);

        binder.forField(isActiveCheckbox)
                .bind(HousingExpenseDTO::getIsActive, HousingExpenseDTO::setIsActive);
    }

    private void setupFieldListeners() {
        amountField.addValueChangeListener(event -> updateEquivalents());
        frequencyCombo.addValueChangeListener(event -> updateEquivalents());
    }

    private void updateEquivalents() {
        BigDecimal amount = amountField.getValue();
        Frequency frequency = frequencyCombo.getValue();

        if (amount != null && frequency != null) {
            BigDecimal monthly = frequency.toMonthlyEquivalent(amount);
            BigDecimal annual = frequency.toAnnualEquivalent(amount);

            monthlyEquivalentDisplay.setText(String.format("£%.2f", monthly));
            annualEquivalentDisplay.setText(String.format("£%.2f", annual));
        } else {
            monthlyEquivalentDisplay.setText("£0.00");
            annualEquivalentDisplay.setText("£0.00");
        }
    }

    private void saveExpense() {
        try {
            HousingExpenseDTO dto = currentExpense != null ? currentExpense : new HousingExpenseDTO();
            dto.setUserId(userId);

            binder.writeBean(dto);

            HousingExpenseDTO savedDto;
            if (dto.getExpenseId() == null) {
                savedDto = housingExpenseService.createFromDto(dto);
                showSuccessNotification("Housing expense added successfully");
            } else {
                savedDto = housingExpenseService.updateFromDto(dto);
                showSuccessNotification("Housing expense updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving housing expense: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentExpense = null;
        HousingExpenseDTO emptyDto = new HousingExpenseDTO();
        emptyDto.setIsActive(true);
        emptyDto.setStartDate(LocalDate.now());
        binder.readBean(emptyDto);
        updateEquivalents();
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
