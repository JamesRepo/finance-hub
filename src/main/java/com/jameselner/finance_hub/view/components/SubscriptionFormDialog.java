package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.SubscriptionFrequency;
import com.jameselner.finance_hub.dto.SubscriptionDTO;
import com.jameselner.finance_hub.service.SubscriptionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.select.Select;
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

public class SubscriptionFormDialog extends Dialog {

    private final SubscriptionService subscriptionService;
    private final Binder<SubscriptionDTO> binder = new BeanValidationBinder<>(SubscriptionDTO.class);

    private Long userId;
    private SubscriptionDTO currentSubscription;

    private TextField nameField;
    private BigDecimalField amountField;
    private Select<SubscriptionFrequency> frequencyCombo;
    private DatePicker paymentDatePicker;
    private TextArea descriptionArea;

    private Span monthlyEquivalentDisplay;
    private Span annualEquivalentDisplay;

    @Setter
    private Consumer<SubscriptionDTO> saveListener;

    public SubscriptionFormDialog(final SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("min(600px, 95vw)");
        setMaxHeight("90vh");

        createFormFields();
        setupBinder();
        setupFieldListeners();
    }

    public void open(final Long userId, final SubscriptionDTO subscription) {
        this.userId = userId;
        this.currentSubscription = subscription;

        removeAll();

        H3 title = new H3(subscription != null && subscription.getSubscriptionId() != null ?
                "Edit Subscription" : "Add Subscription");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        VerticalLayout equivalentsSection = createEquivalentsSection();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, equivalentsSection, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (subscription != null) {
            binder.readBean(subscription);
            updateEquivalents();
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        nameField = new TextField("Subscription Name");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("e.g., Netflix, Spotify, Gym");
        nameField.setPlaceholder("Enter subscription name");

        amountField = new BigDecimalField("Amount");
        amountField.setPrefixComponent(new Span("£"));
        amountField.setRequiredIndicatorVisible(true);
        amountField.setHelperText("Cost per billing period");
        amountField.setPlaceholder("0.00");

        frequencyCombo = new Select<>();
        frequencyCombo.setLabel("Billing Frequency");
        frequencyCombo.setItems(SubscriptionFrequency.values());
        frequencyCombo.setItemLabelGenerator(SubscriptionFrequency::getDisplayName);
        frequencyCombo.setRequiredIndicatorVisible(true);
        frequencyCombo.setHelperText("How often are you billed?");

        paymentDatePicker = new DatePicker("Payment Date");
        paymentDatePicker.setRequiredIndicatorVisible(true);
        paymentDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        paymentDatePicker.setHelperText("When did this payment come out of your account?");

        descriptionArea = new TextArea("Notes");
        descriptionArea.setHelperText("Optional: Add any notes about this subscription");
        descriptionArea.setMaxLength(1000);
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(nameField);
        formLayout.setColspan(nameField, 2);

        formLayout.add(
                amountField,
                frequencyCombo,
                paymentDatePicker
        );

        formLayout.add(descriptionArea);
        formLayout.setColspan(descriptionArea, 2);

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
        saveButton.addClickListener(event -> saveSubscription());

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
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Subscription name is required")
                .bind(SubscriptionDTO::getName, SubscriptionDTO::setName);

        binder.forField(amountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(SubscriptionDTO::getAmount, SubscriptionDTO::setAmount);

        binder.forField(frequencyCombo)
                .withValidator(Objects::nonNull, "Frequency is required")
                .bind(SubscriptionDTO::getFrequency, SubscriptionDTO::setFrequency);

        binder.forField(paymentDatePicker)
                .withValidator(Objects::nonNull, "Payment date is required")
                .bind(SubscriptionDTO::getPaymentDate, SubscriptionDTO::setPaymentDate);

        binder.forField(descriptionArea)
                .bind(SubscriptionDTO::getDescription, SubscriptionDTO::setDescription);
    }

    private void setupFieldListeners() {
        amountField.addValueChangeListener(event -> updateEquivalents());
        frequencyCombo.addValueChangeListener(event -> updateEquivalents());
    }

    private void updateEquivalents() {
        BigDecimal amount = amountField.getValue();
        SubscriptionFrequency frequency = frequencyCombo.getValue();

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

    private void saveSubscription() {
        try {
            SubscriptionDTO dto = currentSubscription != null ? currentSubscription : new SubscriptionDTO();
            dto.setUserId(userId);

            binder.writeBean(dto);

            SubscriptionDTO savedDto;
            if (dto.getSubscriptionId() == null) {
                savedDto = subscriptionService.createFromDto(dto);
                showSuccessNotification("Subscription added successfully");
            } else {
                savedDto = subscriptionService.updateFromDto(dto);
                showSuccessNotification("Subscription updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving subscription: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentSubscription = null;
        SubscriptionDTO emptyDto = new SubscriptionDTO();
        emptyDto.setPaymentDate(LocalDate.now().withDayOfMonth(1));
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
