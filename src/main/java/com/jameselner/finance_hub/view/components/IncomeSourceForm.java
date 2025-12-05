package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.IncomeSourceService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IncomeSourceForm extends VerticalLayout {

    private final IncomeSourceService incomeSourceService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final Binder<IncomeSourceDTO> binder = new BeanValidationBinder<>(IncomeSourceDTO.class);
    private IncomeSourceDTO currentIncomeSource;

    private TextField sourceNameField;
    private TextArea descriptionField;
    private BigDecimalField amountField;
    private Checkbox isRecurringCheckbox;
    private ComboBox<RecurrenceFrequency> recurrenceFrequencyCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private ComboBox<Category> categoryCombo;
    private Checkbox isActiveCheckbox;
    private Checkbox autoCreateTransactionCheckbox;

    private Button saveButton;
    private Button cancelButton;

    @Setter
    private Consumer<IncomeSourceDTO> saveListener;
    @Setter
    private Runnable cancelListener;

    public IncomeSourceForm(
            final IncomeSourceService incomeSourceService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository,
            final CategoryRepository categoryRepository
    ) {
        this.incomeSourceService = incomeSourceService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;

        addClassName("income-source-form");
        setSpacing(true);
        setPadding(true);

        createFormFields();
        createButtons();

        add(createFormLayout(), createButtonLayout());

        setupBinder();
        setupFieldListeners();
    }

    private void createFormFields() {
        sourceNameField = new TextField("Income Source Name");
        sourceNameField.setRequiredIndicatorVisible(true);
        sourceNameField.setHelperText("e.g., Full-time Salary, Freelance Work, Rental Income");
        sourceNameField.setPlaceholder("Enter source name");

        descriptionField = new TextArea("Description");
        descriptionField.setHelperText("Optional details about this income source");
        descriptionField.setMaxLength(1000);

        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Expected income amount per occurrence");

        isRecurringCheckbox = new Checkbox("Recurring Income");
        isRecurringCheckbox.setHelperText("Check if this income repeats regularly");

        recurrenceFrequencyCombo = new ComboBox<>("Frequency");
        recurrenceFrequencyCombo.setItems(
                RecurrenceFrequency.WEEKLY,
                RecurrenceFrequency.BI_WEEKLY,
                RecurrenceFrequency.MONTHLY,
                RecurrenceFrequency.QUARTERLY,
                RecurrenceFrequency.SEMI_ANNUALLY,
                RecurrenceFrequency.ANNUALLY
        );
        recurrenceFrequencyCombo.setItemLabelGenerator(RecurrenceFrequency::getDisplayName);
        recurrenceFrequencyCombo.setHelperText("How often does this income occur?");
        recurrenceFrequencyCombo.setEnabled(false);

        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setRequiredIndicatorVisible(true);
        startDatePicker.setValue(LocalDate.now());
        startDatePicker.setHelperText("When did/will this income start?");

        endDatePicker = new DatePicker("End Date");
        endDatePicker.setHelperText("Optional: When will this income end?");

        List<Category> incomeCategories = categoryRepository.findByCategoryType(CategoryType.INCOME);
        categoryCombo = new ComboBox<>("Category");
        categoryCombo.setItems(incomeCategories);
        categoryCombo.setItemLabelGenerator(Category::getCategoryName);
        categoryCombo.setHelperText("Optional: Categorize this income");

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive sources won't be included in forecasts");

        autoCreateTransactionCheckbox = new Checkbox("Auto-create Transactions");
        autoCreateTransactionCheckbox.setValue(false);
        autoCreateTransactionCheckbox.setHelperText("Automatically create transaction when income is due");
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(sourceNameField);
        formLayout.setColspan(sourceNameField, 2);

        formLayout.add(descriptionField);
        formLayout.setColspan(descriptionField, 2);

        formLayout.add(
                amountField,
                categoryCombo,
                isRecurringCheckbox,
                recurrenceFrequencyCombo,
                startDatePicker,
                endDatePicker,
                isActiveCheckbox,
                autoCreateTransactionCheckbox
        );

        return formLayout;
    }

    private void createButtons() {
        saveButton = new Button("Save Income Source");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveIncomeSource());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> cancel());
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(sourceNameField)
                .withValidator(Objects::nonNull, "Source name is required")
                .withValidator(name -> !name.trim().isEmpty(), "Source name cannot be empty")
                .bind(IncomeSourceDTO::getSourceName, IncomeSourceDTO::setSourceName);

        binder.forField(descriptionField)
                .bind(IncomeSourceDTO::getDescription, IncomeSourceDTO::setDescription);

        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(IncomeSourceDTO::getAmount, IncomeSourceDTO::setAmount);

        binder.forField(isRecurringCheckbox)
                .bind(IncomeSourceDTO::getIsRecurring, IncomeSourceDTO::setIsRecurring);

        binder.forField(recurrenceFrequencyCombo)
                .bind(IncomeSourceDTO::getRecurrenceFrequency, IncomeSourceDTO::setRecurrenceFrequency);

        binder.forField(startDatePicker)
                .withValidator(Objects::nonNull, "Start date is required")
                .bind(IncomeSourceDTO::getStartDate, IncomeSourceDTO::setStartDate);

        binder.forField(endDatePicker)
                .bind(IncomeSourceDTO::getEndDate, IncomeSourceDTO::setEndDate);

        binder.forField(categoryCombo)
                .bind(
                        dto -> {
                            if (dto.getCategoryId() != null) {
                                return categoryRepository.findById(dto.getCategoryId()).orElse(null);
                            }
                            return null;
                        },
                        (dto, category) -> {
                            if (category != null) {
                                dto.setCategoryId(category.getCategoryId());
                                dto.setCategoryName(category.getCategoryName());
                            } else {
                                dto.setCategoryId(null);
                                dto.setCategoryName(null);
                            }
                        }
                );

        binder.forField(isActiveCheckbox)
                .bind(IncomeSourceDTO::getIsActive, IncomeSourceDTO::setIsActive);

        binder.forField(autoCreateTransactionCheckbox)
                .bind(IncomeSourceDTO::getAutoCreateTransaction, IncomeSourceDTO::setAutoCreateTransaction);
    }

    private void setupFieldListeners() {
        isRecurringCheckbox.addValueChangeListener(event -> {
            boolean isRecurring = event.getValue();
            recurrenceFrequencyCombo.setEnabled(isRecurring);
            recurrenceFrequencyCombo.setRequiredIndicatorVisible(isRecurring);

            if (!isRecurring) {
                recurrenceFrequencyCombo.clear();
            }
        });
    }

    private void saveIncomeSource() {
        try {
            IncomeSourceDTO dto = currentIncomeSource != null ? currentIncomeSource : new IncomeSourceDTO();

            User currentUser = getCurrentUser();
            dto.setUserId(currentUser.getUserId());

            binder.writeBean(dto);

            IncomeSourceDTO savedDto;
            if (dto.getIncomeSourceId() == null) {
                savedDto = incomeSourceService.createIncomeSourceFromDto(dto);
                showSuccessNotification("Income source created successfully");
            } else {
                savedDto = incomeSourceService.updateIncomeSourceFromDto(dto);
                showSuccessNotification("Income source updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving income source: " + e.getMessage());
        }
    }

    private void cancel() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    public void setIncomeSource(final IncomeSourceDTO dto) {
        this.currentIncomeSource = dto;
        binder.readBean(dto);

        if (dto.getIsRecurring() != null && dto.getIsRecurring()) {
            recurrenceFrequencyCombo.setEnabled(true);
        }
    }

    public void clearForm() {
        this.currentIncomeSource = null;
        IncomeSourceDTO emptyDto = new IncomeSourceDTO();
        emptyDto.setIsRecurring(false);
        emptyDto.setIsActive(true);
        emptyDto.setAutoCreateTransaction(false);
        emptyDto.setStartDate(LocalDate.now());
        binder.readBean(emptyDto);
        recurrenceFrequencyCombo.setEnabled(false);
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
