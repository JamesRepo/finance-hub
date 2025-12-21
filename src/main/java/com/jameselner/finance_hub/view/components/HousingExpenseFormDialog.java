package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.HousingExpenseType;
import com.jameselner.finance_hub.dto.HousingExpenseDTO;
import com.jameselner.finance_hub.service.HousingExpenseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class HousingExpenseFormDialog extends Dialog {

    private final HousingExpenseService housingExpenseService;

    private Long userId;
    private HousingExpenseDTO currentExpense;

    private ComboBox<HousingExpenseType> expenseTypeCombo;
    private BigDecimalField amountField;
    private ComboBox<Month> monthCombo;
    private ComboBox<Integer> yearCombo;

    @Setter
    private Consumer<HousingExpenseDTO> saveListener;

    public HousingExpenseFormDialog(final HousingExpenseService housingExpenseService) {
        this.housingExpenseService = housingExpenseService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("min(500px, 95vw)");
        setMaxHeight("90vh");

        createFormFields();
    }

    public void open(final Long userId, final HousingExpenseDTO expense) {
        this.userId = userId;
        this.currentExpense = expense;

        removeAll();

        H3 title = new H3(expense != null && expense.getExpenseId() != null ?
                "Edit Housing Expense" : "Add Housing Expense");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (expense != null && expense.getExpenseId() != null) {
            populateForm(expense);
        } else {
            clearForm();
            // If expense has a month set (e.g., from adding to specific month), use it
            if (expense != null && expense.getExpenseMonth() != null) {
                monthCombo.setValue(expense.getExpenseMonth().getMonth());
                yearCombo.setValue(expense.getExpenseMonth().getYear());
            }
        }

        open();
    }

    private void createFormFields() {
        expenseTypeCombo = new ComboBox<>("Expense Type");
        expenseTypeCombo.setItems(HousingExpenseType.values());
        expenseTypeCombo.setItemLabelGenerator(HousingExpenseType::getDisplayName);
        expenseTypeCombo.setRequiredIndicatorVisible(true);
        expenseTypeCombo.setWidthFull();

        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setWidthFull();

        monthCombo = new ComboBox<>("Month");
        monthCombo.setItems(Month.values());
        monthCombo.setItemLabelGenerator(m -> m.getDisplayName(TextStyle.FULL, Locale.UK));
        monthCombo.setRequiredIndicatorVisible(true);
        monthCombo.setValue(LocalDate.now().getMonth());

        yearCombo = new ComboBox<>("Year");
        int currentYear = LocalDate.now().getYear();
        yearCombo.setItems(IntStream.rangeClosed(currentYear - 5, currentYear + 1).boxed().toList());
        yearCombo.setRequiredIndicatorVisible(true);
        yearCombo.setValue(currentYear);
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        formLayout.add(expenseTypeCombo);
        formLayout.setColspan(expenseTypeCombo, 2);

        formLayout.add(amountField);
        formLayout.setColspan(amountField, 2);

        formLayout.add(monthCombo, yearCombo);

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

    private void populateForm(final HousingExpenseDTO expense) {
        expenseTypeCombo.setValue(expense.getExpenseType());
        amountField.setValue(expense.getAmount());
        if (expense.getExpenseMonth() != null) {
            monthCombo.setValue(expense.getExpenseMonth().getMonth());
            yearCombo.setValue(expense.getExpenseMonth().getYear());
        }
    }

    private void saveExpense() {
        // Validate fields
        if (expenseTypeCombo.getValue() == null) {
            showErrorNotification("Please select an expense type");
            return;
        }
        if (amountField.getValue() == null || amountField.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            showErrorNotification("Please enter a valid amount");
            return;
        }
        if (monthCombo.getValue() == null || yearCombo.getValue() == null) {
            showErrorNotification("Please select a month and year");
            return;
        }

        try {
            HousingExpenseDTO dto = currentExpense != null && currentExpense.getExpenseId() != null
                    ? currentExpense
                    : new HousingExpenseDTO();

            dto.setUserId(userId);
            dto.setExpenseType(expenseTypeCombo.getValue());
            dto.setAmount(amountField.getValue());
            dto.setExpenseMonth(LocalDate.of(yearCombo.getValue(), monthCombo.getValue(), 1));

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

        } catch (Exception e) {
            showErrorNotification("Error saving housing expense: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentExpense = null;
        expenseTypeCombo.clear();
        amountField.clear();
        monthCombo.setValue(LocalDate.now().getMonth());
        yearCombo.setValue(LocalDate.now().getYear());
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
