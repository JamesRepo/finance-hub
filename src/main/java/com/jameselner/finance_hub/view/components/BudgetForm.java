package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.BudgetService;
import com.jameselner.finance_hub.service.CategoryService;
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
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BudgetForm extends VerticalLayout {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private final Binder<BudgetDTO> binder = new BeanValidationBinder<>(BudgetDTO.class);
    private BudgetDTO currentBudget;

    // Form components
    private BigDecimalField amountField;
    private ComboBox<Category> categoryField;
    private ComboBox<PeriodType> periodTypeField;
    private DatePicker startDateField;
    private DatePicker endDateField;

    private Button saveButton;
    private Button cancelButton;

    @Setter
    private Consumer<BudgetDTO> saveListener;
    @Setter
    private Runnable cancelListener;

    public BudgetForm(
            final BudgetService budgetService,
            final CategoryService categoryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        addClassName("budget-form");
        setSpacing(true);
        setPadding(true);

        createFormFields();
        createButtons();

        add(createFormLayout(), createButtonLayout());

        setupBinder();
    }

    private void createFormFields() {
        amountField = new BigDecimalField("Budget Amount");
        amountField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Enter the budget amount for this period");

        categoryField = new ComboBox<>("Category");
        categoryField.setItemLabelGenerator(Category::getCategoryName);
        categoryField.setRequiredIndicatorVisible(true);
        categoryField.setHelperText("Select expense category to budget for");
        // Only show expense categories
        List<Category> expenseCategories = categoryService.findByType(CategoryType.EXPENSE);
        categoryField.setItems(expenseCategories);

        periodTypeField = new ComboBox<>("Period Type");
        periodTypeField.setItems(PeriodType.values());
        periodTypeField.setRequiredIndicatorVisible(true);
        periodTypeField.setHelperText("Budget period duration");
        periodTypeField.setValue(PeriodType.MONTHLY);

        // Add listener to auto-calculate dates when period type changes
        periodTypeField.addValueChangeListener(event -> {
            if (event.getValue() != null && startDateField.getValue() != null) {
                updateEndDateBasedOnPeriod(startDateField.getValue(), event.getValue());
            }
        });

        startDateField = new DatePicker("Start Date");
        startDateField.setRequiredIndicatorVisible(true);
        startDateField.setValue(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));

        // Add listener to auto-calculate end date when start date changes
        startDateField.addValueChangeListener(event -> {
            if (event.getValue() != null && periodTypeField.getValue() != null) {
                updateEndDateBasedOnPeriod(event.getValue(), periodTypeField.getValue());
            }
        });

        endDateField = new DatePicker("End Date");
        endDateField.setRequiredIndicatorVisible(true);
        endDateField.setValue(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
    }

    private void updateEndDateBasedOnPeriod(final LocalDate startDate, final PeriodType periodType) {
        LocalDate endDate = switch (periodType) {
            case WEEKLY -> startDate.plusWeeks(1).minusDays(1);
            case MONTHLY -> startDate.plusMonths(1).minusDays(1);
            case QUARTERLY -> startDate.plusMonths(3).minusDays(1);
            case YEARLY -> startDate.plusYears(1).minusDays(1);
        };
        endDateField.setValue(endDate);
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(
                amountField,
                categoryField,
                periodTypeField,
                startDateField,
                endDateField
        );

        return formLayout;
    }

    private void createButtons() {
        saveButton = new Button("Save Budget");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveBudget());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> cancel());
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        return buttonLayout;
    }

    private void setupBinder() {
        // Bind amount field with validation
        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(BudgetDTO::getAmount, BudgetDTO::setAmount);

        // Bind category field with validation
        binder.forField(categoryField)
                .withValidator(Objects::nonNull, "Category is required")
                .bind(
                        dto -> dto.getCategoryId() != null ?
                                categoryService.findById(dto.getCategoryId()).orElse(null) : null,
                        (dto, category) -> {
                            if (category != null) {
                                dto.setCategoryId(category.getCategoryId());
                                dto.setCategoryName(category.getCategoryName());
                                dto.setCategoryColorCode(category.getColorCode());
                            }
                        }
                );

        // Bind period type
        binder.forField(periodTypeField)
                .withValidator(Objects::nonNull, "Period type is required")
                .bind(BudgetDTO::getPeriodType, BudgetDTO::setPeriodType);

        // Bind start date with validation
        binder.forField(startDateField)
                .withValidator(Objects::nonNull, "Start date is required")
                .bind(BudgetDTO::getStartDate, BudgetDTO::setStartDate);

        // Bind end date with validation
        binder.forField(endDateField)
                .withValidator(Objects::nonNull, "End date is required")
                .withValidator(date -> {
                    LocalDate startDate = startDateField.getValue();
                    return startDate == null || !date.isBefore(startDate);
                }, "End date must be after start date")
                .bind(BudgetDTO::getEndDate, BudgetDTO::setEndDate);
    }

    public void setBudget(final BudgetDTO budget) {
        this.currentBudget = budget;

        binder.readBean(Objects.requireNonNullElseGet(budget, BudgetDTO::new));
    }

    public void clearForm() {
        this.currentBudget = new BudgetDTO();
        this.currentBudget.setPeriodType(PeriodType.MONTHLY);
        this.currentBudget.setStartDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
        this.currentBudget.setEndDate(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));

        binder.readBean(currentBudget);
    }

    private void saveBudget() {
        try {
            if (currentBudget == null) {
                currentBudget = new BudgetDTO();
            }

            // Write form values to DTO
            binder.writeBean(currentBudget);

            // Set user ID (from current session)
            if (currentBudget.getUserId() == null) {
                User currentUser = getCurrentUser();
                currentBudget.setUserId(currentUser.getUserId());
            }

            // Save via service
            BudgetDTO savedBudget;
            if (currentBudget.getBudgetId() == null) {
                savedBudget = budgetService.createBudgetFromDto(currentBudget);
                showSuccessNotification("Budget created successfully!");
            } else {
                savedBudget = budgetService.updateBudgetFromDto(currentBudget);
                showSuccessNotification("Budget updated successfully!");
            }

            // Notify listener
            if (saveListener != null) {
                saveListener.accept(savedBudget);
            }

            clearForm();

        } catch (ValidationException e) {
            showErrorNotification("Please fix the validation errors in the form.");
        } catch (Exception e) {
            showErrorNotification("Error saving budget: " + e.getMessage());
        }
    }

    private void cancel() {
        clearForm();
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    private void showSuccessNotification(final String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(final String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * Pre-fill form with data from a specific month
     */
    public void copyFromMonth(final YearMonth yearMonth) {
        BudgetDTO newBudget = new BudgetDTO();
        newBudget.setPeriodType(PeriodType.MONTHLY);
        newBudget.setStartDate(yearMonth.atDay(1));
        newBudget.setEndDate(yearMonth.atEndOfMonth());

        if (currentBudget != null && currentBudget.getCategoryId() != null) {
            newBudget.setCategoryId(currentBudget.getCategoryId());
            newBudget.setCategoryName(currentBudget.getCategoryName());
            newBudget.setAmount(currentBudget.getAmount());
        }

        setBudget(newBudget);
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
