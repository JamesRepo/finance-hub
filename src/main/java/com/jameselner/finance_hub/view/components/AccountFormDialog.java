package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.AccountType;
import com.jameselner.finance_hub.dto.AccountDTO;
import com.jameselner.finance_hub.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

public class AccountFormDialog extends Dialog {

    private final AccountService accountService;
    private final Binder<AccountDTO> binder = new BeanValidationBinder<>(AccountDTO.class);

    private Long userId;
    private AccountDTO currentAccount;

    private TextField accountNameField;
    private ComboBox<AccountType> accountTypeCombo;
    private BigDecimalField balanceField;
    private TextField currencyField;
    private Checkbox isActiveCheckbox;

    @Setter
    private Consumer<AccountDTO> saveListener;

    public AccountFormDialog(final AccountService accountService) {
        this.accountService = accountService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("500px");

        createFormFields();
        setupBinder();
    }

    public void open(final Long userId, final AccountDTO account) {
        this.userId = userId;
        this.currentAccount = account;

        removeAll();

        H3 title = new H3(account != null && account.getAccountId() != null ?
                "Edit Account" : "Add Account");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (account != null) {
            binder.readBean(account);
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        accountNameField = new TextField("Account Name");
        accountNameField.setRequiredIndicatorVisible(true);
        accountNameField.setHelperText("e.g., Main Current Account, Savings");
        accountNameField.setPlaceholder("Enter account name");

        accountTypeCombo = new ComboBox<>("Account Type");
        accountTypeCombo.setItems(AccountType.values());
        accountTypeCombo.setItemLabelGenerator(type -> {
            return switch (type) {
                case CURRENT -> "Current Account";
                case SAVINGS -> "Savings Account";
                case CREDIT_CARD -> "Credit Card";
                case INVESTMENT -> "Investment Account";
                case CASH -> "Cash";
                case IMPORTED -> "Imported Account";
                case OTHER -> "Other";
            };
        });
        accountTypeCombo.setRequiredIndicatorVisible(true);
        accountTypeCombo.setHelperText("Select the type of account");

        balanceField = new BigDecimalField("Initial Balance");
        balanceField.setPrefixComponent(new com.vaadin.flow.component.html.Span("£"));
        balanceField.setHelperText("Current balance in this account");
        balanceField.setPlaceholder("0.00");
        balanceField.setValue(BigDecimal.ZERO);

        currencyField = new TextField("Currency");
        currencyField.setValue("GBP");
        currencyField.setHelperText("3-letter currency code");
        currencyField.setMaxLength(3);

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive accounts won't appear in dropdowns");
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(accountNameField);
        formLayout.setColspan(accountNameField, 2);

        formLayout.add(
                accountTypeCombo,
                balanceField,
                currencyField,
                isActiveCheckbox
        );

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveAccount());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(accountNameField)
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Account name is required")
                .bind(AccountDTO::getAccountName, AccountDTO::setAccountName);

        binder.forField(accountTypeCombo)
                .withValidator(Objects::nonNull, "Account type is required")
                .bind(AccountDTO::getAccountType, AccountDTO::setAccountType);

        binder.forField(balanceField)
                .withValidator(new BigDecimalRangeValidator(
                        "Balance must be a valid amount",
                        new BigDecimal("-999999999.99"),
                        new BigDecimal("999999999.99")))
                .bind(AccountDTO::getBalance, AccountDTO::setBalance);

        binder.forField(currencyField)
                .withValidator(currency -> currency != null && currency.length() == 3, "Currency must be 3 characters")
                .bind(AccountDTO::getCurrency, AccountDTO::setCurrency);

        binder.forField(isActiveCheckbox)
                .bind(AccountDTO::getIsActive, AccountDTO::setIsActive);
    }

    private void saveAccount() {
        try {
            AccountDTO dto = currentAccount != null ? currentAccount : new AccountDTO();
            dto.setUserId(userId);

            binder.writeBean(dto);

            AccountDTO savedDto;
            if (dto.getAccountId() == null) {
                savedDto = accountService.createFromDto(dto);
                showSuccessNotification("Account added successfully");
            } else {
                savedDto = accountService.updateFromDto(dto);
                showSuccessNotification("Account updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving account: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentAccount = null;
        AccountDTO emptyDto = new AccountDTO();
        emptyDto.setBalance(BigDecimal.ZERO);
        emptyDto.setCurrency("GBP");
        emptyDto.setIsActive(true);
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
