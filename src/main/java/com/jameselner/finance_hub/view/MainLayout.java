package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.view.components.AppHeader;
import com.jameselner.finance_hub.view.components.AppNavigation;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Main application layout using Vaadin's AppLayout.
 * This layout provides:
 * - A fixed header with logo and user actions
 * - A collapsible sidebar navigation drawer
 * - Auto-close drawer on mobile after navigation
 */
@CssImport("./styles/finance-dashboard.css")
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private static final int MOBILE_BREAKPOINT = 768;

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

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Close drawer on mobile after navigation
        if (isDrawerOpened()) {
            closeDrawerOnMobile();
        }
    }

    private void closeDrawerOnMobile() {
        getUI().ifPresent(ui -> {
            Page page = ui.getPage();
            page.retrieveExtendedClientDetails(details -> {
                if (details.getScreenWidth() <= MOBILE_BREAKPOINT) {
                    setDrawerOpened(false);
                }
            });
        });
    }
}