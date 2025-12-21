package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.DashboardSummaryDto;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.DashboardService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Component displaying summary cards with key financial metrics.
 * Shows Total Balance, Monthly Income, Monthly Expenses, and Savings Rate.
 * Each card includes an icon, value, label, and trend indicator.
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

                    // Create the four summary cards with real data
                    add(createSummaryCard(
                            "Total Balance",
                            formatCurrency(summary.totalBalance()),
                            "All active accounts",
                            VaadinIcon.WALLET,
                            "#2563eb",
                            true
                    ));

                    BigDecimal incomeChange = summary.getIncomeChange();
                    add(createSummaryCard(
                            "Monthly Income",
                            formatCurrency(summary.monthlyIncome()),
                            formatPercentageChange(incomeChange) + " from last month",
                            VaadinIcon.TRENDING_UP,
                            "#10b981",
                            incomeChange.compareTo(BigDecimal.ZERO) >= 0
                    ));

                    BigDecimal expenseChange = summary.getExpenseChange();
                    add(createSummaryCard(
                            "Monthly Expenses",
                            formatCurrency(summary.monthlyExpenses()),
                            formatPercentageChange(expenseChange) + " from last month",
                            VaadinIcon.TRENDING_DOWN,
                            "#ef4444",
                            expenseChange.compareTo(BigDecimal.ZERO) <= 0  // Lower expenses is positive
                    ));

                    add(createSummaryCard(
                            "Total Debt",
                            formatCurrency(summary.totalDebt()),
                            "Current month",
                            VaadinIcon.BAN,
                            "#f59e0b",
                            true
                    ));
                });
    }

    private String formatCurrency(final BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.UK);
        return formatter.format(amount);
    }

    private String formatPercentage(final BigDecimal percentage) {
        return String.format("%.1f%%", percentage);
    }

    private String formatPercentageChange(final BigDecimal change) {
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + formatPercentage(change);
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return formatPercentage(change);
        } else {
            return "No change";
        }
    }

    /**
     * Creates an individual summary card with metric information.
     *
     * @param label The card label (e.g., "Total Balance")
     * @param value The main value to display (e.g., "$24,582.00")
     * @param trend Trend or additional information text
     * @param iconType The Vaadin icon to display
     * @param iconColor The color for the icon background
     * @param isPositive Whether the trend is positive (green) or not (red)
     * @return A Div containing the formatted card
     */
    private Div createSummaryCard(String label, String value, String trend,
                                  VaadinIcon iconType, String iconColor, boolean isPositive) {
        Div card = new Div();
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "0.75rem")
                .set("padding", "1.5rem")
                .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.1)")
                .set("border", "1px solid #e2e8f0");

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
                .set("color", "#1e293b")
                .set("display", "block")
                .set("margin-bottom", "0.25rem");

        // Label text
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "#64748b")
                .set("font-size", "0.875rem")
                .set("display", "block")
                .set("margin-bottom", "0.75rem");

        // Trend indicator with arrow
        HorizontalLayout trendLayout = new HorizontalLayout();
        trendLayout.setSpacing(true);
        trendLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        trendLayout.setPadding(false);
        trendLayout.getStyle()
                .set("margin", "0");

        Icon trendIcon;
        if (isPositive) {
            trendIcon = VaadinIcon.ARROW_UP.create();
            trendIcon.setColor("#10b981");
        } else {
            trendIcon = VaadinIcon.ARROW_DOWN.create();
            trendIcon.setColor("#ef4444");
        }
        trendIcon.setSize("14px");

        Span trendText = new Span(trend);
        trendText.getStyle()
                .set("font-size", "0.875rem")
                .set("color", isPositive ? "#10b981" : "#ef4444");

        trendLayout.add(trendIcon, trendText);

        // Assemble the card
        card.add(iconContainer, valueSpan, labelSpan, trendLayout);

        return card;
    }
}
