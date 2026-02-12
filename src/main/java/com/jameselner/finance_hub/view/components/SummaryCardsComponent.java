package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.DashboardSummaryDto;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.DashboardService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Component displaying summary cards with key financial metrics.
 * Shows Total Balance, Last Monthly Income, Monthly Expenses, Total Debt, and Total Savings.
 * Each card includes an icon, value, and label.
 */
public class SummaryCardsComponent extends HorizontalLayout {

    public SummaryCardsComponent(
            final DashboardService dashboardService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        setWidthFull();
        setSpacing(true);
        addClassName("responsive-cards");
        addClassName("summary-cards-grid");
        getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(250px, 1fr))")
                .set("gap", "1.5rem")
                .set("margin-bottom", "2rem");

        // Get current user and load dashboard data
        authenticationContext.getAuthenticatedUser(UserDetails.class)
                .ifPresent(userDetails -> {
                    User user = userRepository.findByEmail(userDetails.getUsername())
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    DashboardSummaryDto summary = dashboardService.getDashboardSummary(user);

                    // Create the five summary cards with real data
                    add(createSummaryCard(
                            "Total Balance",
                            formatCurrency(summary.totalBalance()),
                            VaadinIcon.WALLET,
                            "var(--finance-primary)"
                    ));

                    add(createSummaryCard(
                            "Last Monthly Income",
                            formatCurrency(summary.lastMonthlyIncome()),
                            VaadinIcon.TRENDING_UP,
                            "var(--finance-secondary)"
                    ));

                    add(createSummaryCard(
                            "Monthly Expenses",
                            formatCurrency(summary.monthlyExpenses()),
                            VaadinIcon.TRENDING_DOWN,
                            "var(--finance-danger)"
                    ));

                    add(createSummaryCard(
                            "Total Debt",
                            formatCurrency(summary.totalDebt()),
                            VaadinIcon.BAN,
                            "var(--finance-warning)"
                    ));

                    add(createSummaryCard(
                            "Total Savings",
                            formatCurrency(summary.totalSavings()),
                            VaadinIcon.PIGGY_BANK,
                            "var(--finance-secondary)"
                    ));
                });
    }

    private String formatCurrency(final BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.UK);
        return formatter.format(amount);
    }

    /**
     * Creates an individual summary card with metric information.
     *
     * @param label The card label (e.g., "Total Balance")
     * @param value The main value to display (e.g., "$24,582.00")
     * @param iconType The Vaadin icon to display
     * @param iconColor The color for the icon background
     * @return A Div containing the formatted card
     */
    private Div createSummaryCard(
            final String label,
            final String value,
            final VaadinIcon iconType,
            final String iconColor
    ) {
        Div card = new Div();
        card.getStyle()
                .set("background-color", "var(--finance-card-bg)")
                .set("border-radius", "0.75rem")
                .set("padding", "1.5rem")
                .set("box-shadow", "var(--finance-shadow)")
                .set("border", "1px solid var(--finance-border)");

        // Icon container with colored background
        Div iconContainer = new Div();
        iconContainer.getStyle()
                .set("width", "48px")
                .set("height", "48px")
                .set("border-radius", "0.75rem")
                .set("background-color", iconColor)
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("margin-bottom", "1rem");

        Icon icon = iconType.create();
        icon.setSize("24px");
        icon.setColor("white");
        iconContainer.add(icon);

        // Main value display
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "1.875rem")
                .set("font-weight", "700")
                .set("color", "var(--finance-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "0.25rem");

        // Label text
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--finance-text-secondary)")
                .set("font-size", "0.875rem")
                .set("display", "block")
                .set("margin-bottom", "0.75rem");

        // Assemble the card
        card.add(iconContainer, valueSpan, labelSpan);

        return card;
    }
}
