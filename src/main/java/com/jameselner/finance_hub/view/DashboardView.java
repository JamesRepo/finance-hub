package com.jameselner.finance_hub.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Finance Hub")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final AuthenticationContext authenticationContext;

    public DashboardView(final AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Welcome to Finance Hub");

        String username = authenticationContext.getPrincipalName().orElse("User");
        Paragraph welcomeMessage = new Paragraph("Hello, " + username + "!");
        Paragraph description = new Paragraph("This is your financial dashboard. Manage your accounts, track transactions, and monitor your budgets all in one place.");

        add(title, welcomeMessage, description);
    }
}
