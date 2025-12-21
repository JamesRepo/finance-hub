package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.UserSettings;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.security.PasswordValidator;
import com.jameselner.finance_hub.service.UserService;
import com.jameselner.finance_hub.service.UserSettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.LinkedHashMap;
import java.util.Map;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings | Finance Hub")
@PermitAll
public class SettingsView extends VerticalLayout {

    private final UserService userService;
    private final UserSettingsService userSettingsService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private TextField firstNameField;
    private TextField lastNameField;
    private EmailField emailField;

    private ComboBox<String> currencyCombo;

    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;

    private static final Map<String, String> CURRENCIES = new LinkedHashMap<>();

    static {
        CURRENCIES.put("GBP", "British Pound (GBP)");
        CURRENCIES.put("USD", "US Dollar (USD)");
        CURRENCIES.put("EUR", "Euro (EUR)");
        CURRENCIES.put("CAD", "Canadian Dollar (CAD)");
        CURRENCIES.put("AUD", "Australian Dollar (AUD)");
        CURRENCIES.put("JPY", "Japanese Yen (JPY)");
        CURRENCIES.put("CHF", "Swiss Franc (CHF)");
        CURRENCIES.put("CNY", "Chinese Yuan (CNY)");
        CURRENCIES.put("INR", "Indian Rupee (INR)");
        CURRENCIES.put("NZD", "New Zealand Dollar (NZD)");
    }

    public SettingsView(
            final UserService userService,
            final UserSettingsService userSettingsService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.userService = userService;
        this.userSettingsService = userSettingsService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createAccountSection(),
                createPreferencesSection(),
                createPasswordSection()
        );

        loadCurrentSettings();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Settings");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private VerticalLayout createAccountSection() {
        VerticalLayout section = createSection("Account Information", VaadinIcon.USER);

        Paragraph description = new Paragraph("Update your personal information and email address.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "0");

        firstNameField = new TextField("First Name");
        firstNameField.setWidthFull();
        firstNameField.setRequired(true);

        lastNameField = new TextField("Last Name");
        lastNameField.setWidthFull();
        lastNameField.setRequired(true);

        emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(firstNameField, lastNameField, emailField);
        formLayout.setColspan(emailField, 2);

        Button saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveAccountInfo());

        section.add(description, formLayout, saveButton);

        return section;
    }

    private VerticalLayout createPreferencesSection() {
        VerticalLayout section = createSection("Preferences", VaadinIcon.COG);

        Paragraph description = new Paragraph("Configure your display and regional preferences.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "0");

        currencyCombo = new ComboBox<>("Display Currency");
        currencyCombo.setItems(CURRENCIES.keySet());
        currencyCombo.setItemLabelGenerator(CURRENCIES::get);
        currencyCombo.setWidthFull();
        currencyCombo.setMaxWidth("400px");
        currencyCombo.setHelperText("This affects how currency values are displayed throughout the application");

        Button saveButton = new Button("Save Preferences", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> savePreferences());

        section.add(description, currencyCombo, saveButton);

        return section;
    }

    private VerticalLayout createPasswordSection() {
        VerticalLayout section = createSection("Change Password", VaadinIcon.LOCK);

        Paragraph description = new Paragraph("Update your password to keep your account secure.");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "0");

        Span requirementsText = new Span(PasswordValidator.getRequirementsText());
        requirementsText.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        currentPasswordField = new PasswordField("Current Password");
        currentPasswordField.setWidthFull();
        currentPasswordField.setMaxWidth("400px");
        currentPasswordField.setRequired(true);

        newPasswordField = new PasswordField("New Password");
        newPasswordField.setWidthFull();
        newPasswordField.setMaxWidth("400px");
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText(PasswordValidator.getRequirementsText());

        confirmPasswordField = new PasswordField("Confirm New Password");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setMaxWidth("400px");
        confirmPasswordField.setRequired(true);

        VerticalLayout passwordFields = new VerticalLayout(
                currentPasswordField,
                newPasswordField,
                confirmPasswordField
        );
        passwordFields.setSpacing(true);
        passwordFields.setPadding(false);

        Button changePasswordButton = new Button("Change Password", new Icon(VaadinIcon.KEY));
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(event -> changePassword());

        section.add(description, passwordFields, changePasswordButton);

        return section;
    }

    private VerticalLayout createSection(final String title, final VaadinIcon icon) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle()
                .set("background-color", "white")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "0.5rem");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);

        Icon sectionIcon = icon.create();
        sectionIcon.setColor("var(--lumo-primary-color)");
        sectionIcon.setSize("24px");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle().set("margin", "0");

        headerLayout.add(sectionIcon, sectionTitle);
        section.add(headerLayout);

        return section;
    }

    private void loadCurrentSettings() {
        User currentUser = getCurrentUser();
        UserSettings settings = userSettingsService.getSettingsForUser(currentUser);

        firstNameField.setValue(currentUser.getFirstName());
        lastNameField.setValue(currentUser.getLastName());
        emailField.setValue(currentUser.getEmail());

        currencyCombo.setValue(settings.getCurrency());
    }

    private void saveAccountInfo() {
        String firstName = firstNameField.getValue().trim();
        String lastName = lastNameField.getValue().trim();
        String email = emailField.getValue().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showErrorNotification("Please fill in all required fields");
            return;
        }

        try {
            User currentUser = getCurrentUser();
            userService.updateProfile(currentUser, firstName, lastName, email);
            showSuccessNotification("Account information updated successfully");
        } catch (IllegalArgumentException e) {
            showErrorNotification(e.getMessage());
        }
    }

    private void savePreferences() {
        String currency = currencyCombo.getValue();

        if (currency == null || currency.isEmpty()) {
            showErrorNotification("Please select a currency");
            return;
        }

        try {
            User currentUser = getCurrentUser();
            userSettingsService.updateCurrency(currentUser, currency);
            showSuccessNotification("Preferences saved successfully");
        } catch (Exception e) {
            showErrorNotification("Error saving preferences: " + e.getMessage());
        }
    }

    private void changePassword() {
        String currentPassword = currentPasswordField.getValue();
        String newPassword = newPasswordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showErrorNotification("Please fill in all password fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showErrorNotification("New passwords do not match");
            return;
        }

        try {
            User currentUser = getCurrentUser();
            userService.changePassword(currentUser, currentPassword, newPassword);

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showSuccessNotification("Password changed successfully");
        } catch (IllegalArgumentException e) {
            showErrorNotification(e.getMessage());
        }
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
