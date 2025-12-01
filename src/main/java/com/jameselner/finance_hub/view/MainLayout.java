package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.view.components.AppHeader;
import com.jameselner.finance_hub.view.components.AppNavigation;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Main application layout using Vaadin's AppLayout.
 *
 * This layout provides:
 * - A fixed header with logo and user actions
 * - A collapsible sidebar navigation drawer
 */
@CssImport("./styles/finance-dashboard.css")
public class MainLayout extends AppLayout {

    public MainLayout(final AuthenticationContext authenticationContext) {
        // Configure the layout
        configureLayout();

        // Add components
        addToNavbar(new DrawerToggle(), new AppHeader(authenticationContext));
        addToDrawer(new AppNavigation());
    }

    private void configureLayout() {
        addClassName("finance-dashboard");
        setPrimarySection(Section.NAVBAR);
        setDrawerOpened(false); // Start with drawer closed
    }
}