package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.service.HolidayService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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

public class HolidayFormDialog extends Dialog {

    private final HolidayService holidayService;
    private final Binder<HolidayDTO> binder = new BeanValidationBinder<>(HolidayDTO.class);

    private Long userId;
    private HolidayDTO currentHoliday;

    private TextField nameField;
    private TextField destinationField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private BigDecimalField budgetField;
    private TextArea descriptionArea;
    private Checkbox isActiveCheckbox;

    @Setter
    private Consumer<HolidayDTO> saveListener;

    public HolidayFormDialog(final HolidayService holidayService) {
        this.holidayService = holidayService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("min(700px, 95vw)");
        setMaxHeight("90vh");

        createFormFields();
        setupBinder();
    }

    public void open(final Long userId, final HolidayDTO holiday) {
        this.userId = userId;
        this.currentHoliday = holiday;

        removeAll();

        H3 title = new H3(holiday != null && holiday.getHolidayId() != null ?
                "Edit Holiday" : "Add Holiday");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (holiday != null) {
            binder.readBean(holiday);
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        nameField = new TextField("Holiday Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Give your holiday a memorable name");
        nameField.setPlaceholder("e.g., Summer in Greece 2025");

        destinationField = new TextField("Destination");
        destinationField.setRequiredIndicatorVisible(true);
        destinationField.setHelperText("Where are you going?");
        destinationField.setPlaceholder("e.g., Athens, Greece");

        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setRequiredIndicatorVisible(true);
        startDatePicker.setValue(LocalDate.now());
        startDatePicker.setHelperText("When does your holiday start?");

        endDatePicker = new DatePicker("End Date");
        endDatePicker.setRequiredIndicatorVisible(true);
        endDatePicker.setValue(LocalDate.now().plusDays(7));
        endDatePicker.setHelperText("When does your holiday end?");

        budgetField = new BigDecimalField("Budget");
        budgetField.setPrefixComponent(new Span("£"));
        budgetField.setRequiredIndicatorVisible(true);
        budgetField.setHelperText("How much do you plan to spend?");
        budgetField.setPlaceholder("0.00");

        descriptionArea = new TextArea("Description");
        descriptionArea.setHelperText("Optional: Add notes about your holiday");
        descriptionArea.setPlaceholder("e.g., Family vacation, includes hotel and flights");
        descriptionArea.setMaxLength(1000);

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive holidays won't be included in calculations");
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(nameField);
        formLayout.setColspan(nameField, 2);

        formLayout.add(destinationField);
        formLayout.setColspan(destinationField, 2);

        formLayout.add(
                startDatePicker,
                endDatePicker,
                budgetField,
                isActiveCheckbox
        );

        formLayout.add(descriptionArea);
        formLayout.setColspan(descriptionArea, 2);

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveHoliday());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(nameField)
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Holiday name is required")
                .bind(HolidayDTO::getName, HolidayDTO::setName);

        binder.forField(destinationField)
                .withValidator(dest -> dest != null && !dest.trim().isEmpty(), "Destination is required")
                .bind(HolidayDTO::getDestination, HolidayDTO::setDestination);

        binder.forField(startDatePicker)
                .withValidator(Objects::nonNull, "Start date is required")
                .bind(HolidayDTO::getStartDate, HolidayDTO::setStartDate);

        binder.forField(endDatePicker)
                .withValidator(Objects::nonNull, "End date is required")
                .withValidator(
                        endDate -> startDatePicker.getValue() == null || !endDate.isBefore(startDatePicker.getValue()),
                        "End date must not be before start date")
                .bind(HolidayDTO::getEndDate, HolidayDTO::setEndDate);

        binder.forField(budgetField)
                .withValidator(new BigDecimalRangeValidator(
                        "Budget must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(HolidayDTO::getBudget, HolidayDTO::setBudget);

        binder.forField(descriptionArea)
                .bind(HolidayDTO::getDescription, HolidayDTO::setDescription);

        binder.forField(isActiveCheckbox)
                .bind(HolidayDTO::getIsActive, HolidayDTO::setIsActive);
    }

    private void saveHoliday() {
        try {
            HolidayDTO dto = currentHoliday != null ? currentHoliday : new HolidayDTO();
            dto.setUserId(userId);

            binder.writeBean(dto);

            HolidayDTO savedDto;
            if (dto.getHolidayId() == null) {
                savedDto = holidayService.createFromDto(dto);
                showSuccessNotification("Holiday added successfully");
            } else {
                savedDto = holidayService.updateFromDto(dto);
                showSuccessNotification("Holiday updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving holiday: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentHoliday = null;
        HolidayDTO emptyDto = new HolidayDTO();
        emptyDto.setIsActive(true);
        emptyDto.setStartDate(LocalDate.now());
        emptyDto.setEndDate(LocalDate.now().plusDays(7));
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
