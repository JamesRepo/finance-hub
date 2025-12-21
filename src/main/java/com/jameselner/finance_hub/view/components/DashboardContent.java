package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.DashboardService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Main dashboard content component.
 * This separates the content from the layout structure.
 */
public class DashboardContent extends VerticalLayout {

    public DashboardContent(
            final DashboardService dashboardService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();
        getStyle()
                .set("background-color", "#f8fafc")
                .set("overflow-y", "auto");

        add(createPageHeader());
        add(new SummaryCardsComponent(dashboardService, authenticationContext, userRepository));

        // Additional components can be added here as they're developed:
        // add(new ChartAndTransactionsComponent());
        // add(new DebtAndSavingsComponent());
        // add(new YearlySummaryComponent());
    }

    private VerticalLayout createPageHeader() {
        H1 pageTitle = new H1("Financial Overview");
        pageTitle.addClassName("page-title");
        pageTitle.getStyle()
                .set("font-size", "2rem")
                .set("margin-bottom", "0.5rem");

        Span pageSubtitle = new Span("Welcome back! Here's your financial overview.");
        pageSubtitle.getStyle()
                .set("color", "#64748b")
                .set("margin-bottom", "2rem");

        VerticalLayout pageHeader = new VerticalLayout(pageTitle, pageSubtitle);
        pageHeader.setSpacing(false);
        pageHeader.setPadding(false);

        return pageHeader;
    }
}