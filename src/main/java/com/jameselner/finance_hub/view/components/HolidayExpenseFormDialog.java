package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.HolidayExpenseType;
import com.jameselner.finance_hub.dto.HolidayExpenseDTO;
import com.jameselner.finance_hub.service.HolidayExpenseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class HolidayExpenseFormDialog extends Dialog {

    private final HolidayExpenseService holidayExpenseService;
    private final Binder<HolidayExpenseDTO> binder = new BeanValidationBinder<>(HolidayExpenseDTO.class);

    private Long holidayId;
    private HolidayExpenseDTO currentExpense;

    private ComboBox<HolidayExpenseType> expenseTypeCombo;
    private TextField descriptionField;
    private BigDecimalField amountField;
    private DatePicker expenseDatePicker;
    private TextArea notesArea;

    @Setter
    private Consumer<HolidayExpenseDTO> saveListener;

    public HolidayExpenseFormDialog(final HolidayExpenseService holidayExpenseService) {
        this.holidayExpenseService = holidayExpenseService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("600px");

        createFormFields();
        setupBinder();
    }

    public void open(final Long holidayId, final HolidayExpenseDTO expense) {
        this.holidayId = holidayId;
        this.currentExpense = expense;

        removeAll();

        H3 title = new H3(expense != null && expense.getExpenseId() != null ?
                "Edit Expense" : "Add Expense");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (expense != null) {
            binder.readBean(expense);
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        expenseTypeCombo = new ComboBox<>("Expense Type");
        expenseTypeCombo.setItems(HolidayExpenseType.values());
        expenseTypeCombo.setItemLabelGenerator(HolidayExpenseType::getDisplayName);
        expenseTypeCombo.setRequiredIndicatorVisible(true);
        expenseTypeCombo.setHelperText("What type of expense is this?");

        descriptionField = new TextField("Description");
        descriptionField.setRequiredIndicatorVisible(true);
        descriptionField.setHelperText("Brief description of the expense");
        descriptionField.setPlaceholder("e.g., Return flight to Athens");

        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("How much did this cost?");
        amountField.setPlaceholder("0.00");

        expenseDatePicker = new DatePicker("Expense Date");
        expenseDatePicker.setRequiredIndicatorVisible(true);
        expenseDatePicker.setValue(LocalDate.now());
        expenseDatePicker.setHelperText("When was this expense incurred?");

        notesArea = new TextArea("Notes");
        notesArea.setHelperText("Optional: Add additional details");
        notesArea.setPlaceholder("e.g., Booked through Skyscanner");
        notesArea.setMaxLength(1000);
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(expenseTypeCombo);
        formLayout.setColspan(expenseTypeCombo, 2);

        formLayout.add(descriptionField);
        formLayout.setColspan(descriptionField, 2);

        formLayout.add(
                amountField,
                expenseDatePicker
        );

        formLayout.add(notesArea);
        formLayout.setColspan(notesArea, 2);

        return formLayout;
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
                .bind(HolidayExpenseDTO::getExpenseType, HolidayExpenseDTO::setExpenseType);

        binder.forField(descriptionField)
                .withValidator(desc -> desc != null && !desc.trim().isEmpty(), "Description is required")
                .bind(HolidayExpenseDTO::getDescription, HolidayExpenseDTO::setDescription);

        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(HolidayExpenseDTO::getAmount, HolidayExpenseDTO::setAmount);

        binder.forField(expenseDatePicker)
                .withValidator(Objects::nonNull, "Expense date is required")
                .bind(HolidayExpenseDTO::getExpenseDate, HolidayExpenseDTO::setExpenseDate);

        binder.forField(notesArea)
                .bind(HolidayExpenseDTO::getNotes, HolidayExpenseDTO::setNotes);
    }

    private void saveExpense() {
        try {
            HolidayExpenseDTO dto = currentExpense != null ? currentExpense : new HolidayExpenseDTO();
            dto.setHolidayId(holidayId);

            binder.writeBean(dto);

            HolidayExpenseDTO savedDto;
            if (dto.getExpenseId() == null) {
                savedDto = holidayExpenseService.createFromDto(dto);
                showSuccessNotification("Expense added successfully");
            } else {
                savedDto = holidayExpenseService.updateFromDto(dto);
                showSuccessNotification("Expense updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving expense: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentExpense = null;
        HolidayExpenseDTO emptyDto = new HolidayExpenseDTO();
        emptyDto.setExpenseDate(LocalDate.now());
        binder.readBean(emptyDto);
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
