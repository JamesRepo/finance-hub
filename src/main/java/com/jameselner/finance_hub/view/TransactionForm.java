package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.Account;
import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.service.AccountService;
import com.jameselner.finance_hub.service.CategoryService;
import com.jameselner.finance_hub.service.TransactionService;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class TransactionForm extends VerticalLayout {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    private final Binder<TransactionDTO> binder = new BeanValidationBinder<>(TransactionDTO.class);
    private TransactionDTO currentTransaction;

    // Form components
    private Tabs transactionTypeTabs;
    private Tab expenseTab;
    private Tab incomeTab;
    private Tab transferTab;

    private BigDecimalField amountField;
    private ComboBox<Account> accountField;
    private ComboBox<Category> categoryField;
    private DatePicker transactionDateField;
    private TextField placeVenueField;
    private TextArea descriptionField;

    private Button saveButton;
    private Button cancelButton;

    private Consumer<TransactionDTO> saveListener;
    private Runnable cancelListener;

    public TransactionForm(
            final TransactionService transactionService,
            final AccountService accountService,
            final CategoryService categoryService
    ) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;

        addClassName("transaction-form");
        setSpacing(true);
        setPadding(true);

        createTransactionTypeTabs();
        createFormFields();
        createButtons();

        add(transactionTypeTabs, createFormLayout(), createButtonLayout());

        setupBinder();
    }

    private void createTransactionTypeTabs() {
        expenseTab = new Tab("Expense");
        incomeTab = new Tab("Income");
        transferTab = new Tab("Transfer");

        transactionTypeTabs = new Tabs(expenseTab, incomeTab, transferTab);
        transactionTypeTabs.setWidthFull();

        transactionTypeTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            TransactionType type = getTransactionTypeFromTab(selectedTab);

            if (currentTransaction != null) {
                currentTransaction.setTransactionType(type);
                updateCategoryFieldItems(type);
            }
        });
    }

    private TransactionType getTransactionTypeFromTab(Tab tab) {
        if (tab == expenseTab) {
            return TransactionType.EXPENSE;
        } else if (tab == incomeTab) {
            return TransactionType.INCOME;
        } else {
            return TransactionType.TRANSFER;
        }
    }

    private void setTabFromTransactionType(TransactionType type) {
        switch (type) {
            case EXPENSE:
                transactionTypeTabs.setSelectedTab(expenseTab);
                break;
            case INCOME:
                transactionTypeTabs.setSelectedTab(incomeTab);
                break;
            case TRANSFER:
                transactionTypeTabs.setSelectedTab(transferTab);
                break;
        }
    }

    private void createFormFields() {
        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Enter the transaction amount");

        categoryField = new ComboBox<>("Category");
        categoryField.setItemLabelGenerator(Category::getCategoryName);
        categoryField.setRequiredIndicatorVisible(true);

        placeVenueField = new TextField("Place/Venue");
        placeVenueField.setPlaceholder("Where was this?");

        transactionDateField = new DatePicker("Transaction Date");
        transactionDateField.setRequiredIndicatorVisible(true);
        transactionDateField.setValue(LocalDate.now());

        accountField = new ComboBox<>("Account");
        accountField.setItemLabelGenerator(Account::getAccountName);
        accountField.setRequiredIndicatorVisible(true);
        accountField.setItems(accountService.findAll());

        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Enter a brief description");
        descriptionField.setMaxLength(500);
        descriptionField.setHelperText("Optional");
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
                placeVenueField,
                transactionDateField,
                accountField,
                descriptionField
        );

//        formLayout.setColspan(descriptionField, 2);

        return formLayout;
    }

    private void createButtons() {
        saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveTransaction());

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
                .bind(TransactionDTO::getAmount, TransactionDTO::setAmount);

        // Bind account field with validation
        binder.forField(accountField)
                .withValidator(account -> account != null, "Account is required")
                .bind(
                        dto -> dto.getAccountId() != null ?
                                accountService.findById(dto.getAccountId()).orElse(null) : null,
                        (dto, account) -> {
                            if (account != null) {
                                dto.setAccountId(account.getAccountId());
                                dto.setAccountName(account.getAccountName());
                            }
                        }
                );

        // Bind category field with validation
        binder.forField(categoryField)
                .withValidator(category -> category != null, "Category is required")
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

        // Bind transaction date with validation
        binder.forField(transactionDateField)
                .withValidator(date -> date != null, "Transaction date is required")
                .withValidator(date -> !date.isAfter(LocalDate.now().plusDays(1)),
                        "Transaction date cannot be in the future")
                .bind(TransactionDTO::getTransactionDate, TransactionDTO::setTransactionDate);

        // Bind simple fields
        binder.forField(placeVenueField)
                .bind(TransactionDTO::getPlaceVenue, TransactionDTO::setPlaceVenue);

        binder.forField(descriptionField)
                .bind(TransactionDTO::getDescription, TransactionDTO::setDescription);
    }

    private void updateCategoryFieldItems(TransactionType type) {
        CategoryType categoryType = type == TransactionType.INCOME ?
                CategoryType.INCOME : CategoryType.EXPENSE;

        List<Category> categories = categoryService.findByType(categoryType);
        categoryField.setItems(categories);

        // Clear selection if current category doesn't match the type
        Category currentCategory = categoryField.getValue();
        if (currentCategory != null && currentCategory.getCategoryType() != categoryType) {
            categoryField.clear();
        }
    }

    public void setTransaction(TransactionDTO transaction) {
        this.currentTransaction = transaction;

        if (transaction != null) {
            // Set the transaction type tab
            if (transaction.getTransactionType() != null) {
                setTabFromTransactionType(transaction.getTransactionType());
                updateCategoryFieldItems(transaction.getTransactionType());
            } else {
                transaction.setTransactionType(TransactionType.EXPENSE);
                transactionTypeTabs.setSelectedTab(expenseTab);
                updateCategoryFieldItems(TransactionType.EXPENSE);
            }

            binder.readBean(transaction);
        } else {
            binder.readBean(new TransactionDTO());
        }
    }

    public void clearForm() {
        this.currentTransaction = new TransactionDTO();
        this.currentTransaction.setTransactionType(TransactionType.EXPENSE);
        this.currentTransaction.setTransactionDate(LocalDate.now());

        binder.readBean(currentTransaction);
        transactionTypeTabs.setSelectedTab(expenseTab);
        updateCategoryFieldItems(TransactionType.EXPENSE);
    }

    private void saveTransaction() {
        try {
            if (currentTransaction == null) {
                currentTransaction = new TransactionDTO();
            }

            // Set transaction type from selected tab
            currentTransaction.setTransactionType(getTransactionTypeFromTab(
                    transactionTypeTabs.getSelectedTab()));

            // Write form values to DTO
            binder.writeBean(currentTransaction);

            // Save via service
            TransactionDTO savedTransaction;
            if (currentTransaction.getTransactionId() == null) {
                savedTransaction = transactionService.createTransactionFromDto(currentTransaction);
                showSuccessNotification("Transaction created successfully!");
            } else {
                savedTransaction = transactionService.updateTransactionFromDto(currentTransaction);
                showSuccessNotification("Transaction updated successfully!");
            }

            // Notify listener
            if (saveListener != null) {
                saveListener.accept(savedTransaction);
            }

            clearForm();

        } catch (ValidationException e) {
            showErrorNotification("Please fix the validation errors in the form.");
        } catch (Exception e) {
            showErrorNotification("Error saving transaction: " + e.getMessage());
        }
    }

    private void cancel() {
        clearForm();
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public void setSaveListener(Consumer<TransactionDTO> saveListener) {
        this.saveListener = saveListener;
    }

    public void setCancelListener(Runnable cancelListener) {
        this.cancelListener = cancelListener;
    }
}