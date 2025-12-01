package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.DashboardService;
import com.jameselner.finance_hub.view.components.DashboardContent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Finance Hub")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(
            final DashboardService dashboardService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(new DashboardContent(dashboardService, authenticationContext, userRepository));
    }
}