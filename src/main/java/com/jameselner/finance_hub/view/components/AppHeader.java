package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.view.DashboardView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

public class AppHeader extends HorizontalLayout {

    private final AuthenticationContext authenticationContext;

    public AppHeader(final AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background-color", "white")
                .set("border-bottom", "1px solid #e2e8f0")
                .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.1)");

        add(createLogoAndHeader(), createUserSection());
    }

    private HorizontalLayout createLogoAndHeader() {
        HorizontalLayout logo = new HorizontalLayout();
        logo.setAlignItems(FlexComponent.Alignment.CENTER);
        logo.setSpacing(true);

        Span logoText = new Span("\uD83D\uDCB0 FinanceHub");
        logoText.getStyle()
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("color", "#4F6BED")
                .set("letter-spacing", "-0.025em");

        RouterLink logoLink = new RouterLink("", DashboardView.class);
        logoLink.add(logoText);
        logoLink.getStyle()
                .set("text-decoration", "none")
                .set("cursor", "pointer");

        logo.add(logoLink);
        return logo;
    }

    private HorizontalLayout createUserSection() {
        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(),
                click -> authenticationContext.logout());
        logoutButton.addClassName("logout-btn");

        HorizontalLayout userSection = new HorizontalLayout(logoutButton);
        userSection.setSpacing(true);
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);
        userSection.getStyle()
                .set("flex-shrink", "0")
                .set("margin-left", "auto");

        return userSection;
    }
}