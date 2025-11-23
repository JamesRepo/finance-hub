package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.dto.UserRegistrationDto;
import com.jameselner.finance_hub.security.PasswordValidator;
import com.jameselner.finance_hub.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Register | Finance Hub")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

    private final UserService userService;

    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final Button registerButton = new Button("Register");

    public RegistrationView(final UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        H1 title = new H1("Create Account");
        Paragraph subtitle = new Paragraph("Join Finance Hub");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("400px");

        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setErrorMessage("Please enter a valid email address");

        firstNameField.setWidthFull();
        firstNameField.setRequired(true);

        lastNameField.setWidthFull();
        lastNameField.setRequired(true);

        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setHelperText(PasswordValidator.getRequirementsText());

        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.addClickListener(e -> handleRegistration());

        formLayout.add(
            firstNameField,
            lastNameField,
            emailField,
            passwordField,
            confirmPasswordField,
            registerButton
        );

        RouterLink loginLink = new RouterLink("Already have an account? Login here", LoginView.class);

        add(
            title,
            subtitle,
            formLayout,
            loginLink
        );
    }

    private void handleRegistration() {
        if (!validateForm()) {
            return;
        }

        if (!passwordField.getValue().equals(confirmPasswordField.getValue())) {
            showNotification("Passwords do not match", NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            UserRegistrationDto registrationDto = new UserRegistrationDto(
                emailField.getValue().trim(),
                passwordField.getValue(),
                firstNameField.getValue().trim(),
                lastNameField.getValue().trim()
            );

            userService.registerUser(registrationDto);

            showNotification("Registration successful! Please login.", NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (IllegalArgumentException e) {
            showNotification(e.getMessage(), NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            showNotification("Registration failed. Please try again.", NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateForm() {
        if (emailField.getValue() == null || emailField.getValue().trim().isEmpty()) {
            showNotification("Email is required", NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (firstNameField.getValue() == null || firstNameField.getValue().trim().isEmpty()) {
            showNotification("First name is required", NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (lastNameField.getValue() == null || lastNameField.getValue().trim().isEmpty()) {
            showNotification("Last name is required", NotificationVariant.LUMO_ERROR);
            return false;
        }

        PasswordValidator.ValidationResult passwordValidation =
            PasswordValidator.validate(passwordField.getValue());
        if (!passwordValidation.isValid()) {
            showNotification(passwordValidation.getErrorMessage(), NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (confirmPasswordField.getValue() == null || confirmPasswordField.getValue().isEmpty()) {
            showNotification("Please confirm your password", NotificationVariant.LUMO_ERROR);
            return false;
        }

        return true;
    }

    private void showNotification(final String message, final NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }
}
