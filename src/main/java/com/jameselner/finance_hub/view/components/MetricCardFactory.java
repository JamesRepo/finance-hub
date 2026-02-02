package com.jameselner.finance_hub.view.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Factory for creating consistent metric card UI components.
 * Centralizes card creation logic used across summary views.
 */
@Component
public class MetricCardFactory {

    /**
     * Create a metric card displaying a currency value.
     *
     * @param label the card label
     * @param formattedValue the pre-formatted currency value string
     * @param color the accent color (CSS variable or color value)
     * @param icon the Vaadin icon to display
     * @param compact whether to use compact styling
     * @return the configured Div component
     */
    public Div createMetricCard(final String label, final String formattedValue,
                                final String color, final VaadinIcon icon, final boolean compact) {
        Div card = createBaseCard(color, compact);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = createLabelSpan(label);
        Span valueSpan = createValueSpan(formattedValue, compact);

        card.add(cardIcon, labelSpan, valueSpan);
        return card;
    }

    /**
     * Create a metric card displaying a percentage value.
     *
     * @param label the card label
     * @param value the percentage value (will be formatted with %)
     * @param color the accent color
     * @param icon the Vaadin icon to display
     * @return the configured Div component
     */
    public Div createPercentageCard(final String label, final BigDecimal value,
                                    final String color, final VaadinIcon icon) {
        Div card = createBaseCard(color, false);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = createLabelSpan(label);

        String formattedValue = String.format("%.1f%%", value != null ? value : BigDecimal.ZERO);
        Span valueSpan = new Span(formattedValue);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);
        return card;
    }

    /**
     * Create a card showing a change value with directional arrow.
     *
     * @param label the card label
     * @param change the change amount
     * @param changePercent the change percentage
     * @param formattedChange the pre-formatted change amount string
     * @return the configured Div component
     */
    public Div createChangeCard(final String label, final BigDecimal change,
                                final BigDecimal changePercent, final String formattedChange) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "1rem")
                .set("background-color", "var(--finance-card-bg)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        boolean isPositive = change.compareTo(BigDecimal.ZERO) >= 0;
        String color = isPositive ? "var(--lumo-success-color)" : "var(--lumo-error-color)";
        Icon arrow = new Icon(isPositive ? VaadinIcon.ARROW_UP : VaadinIcon.ARROW_DOWN);
        arrow.setColor(color);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "0.5rem");

        HorizontalLayout valueLayout = new HorizontalLayout(arrow);
        valueLayout.setSpacing(true);
        valueLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span changeValue = new Span(formattedChange);
        changeValue.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", color);

        valueLayout.add(changeValue);

        Span percentValue = new Span(String.format("(%.1f%%)", changePercent.abs()));
        percentValue.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", color);

        card.add(labelSpan, valueLayout, percentValue);
        return card;
    }

    /**
     * Create a simple stat card with text value.
     *
     * @param label the card label
     * @param value the text value to display
     * @param icon the Vaadin icon to display
     * @return the configured Div component
     */
    public Div createStatCard(final String label, final String value, final VaadinIcon icon) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", "0.75rem")
                .set("background-color", "var(--finance-card-bg)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid var(--lumo-primary-color)");

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor("var(--lumo-primary-color)");
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = createLabelSpan(label);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);
        return card;
    }

    private Div createBaseCard(final String color, final boolean compact) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1")
                .set("padding", compact ? "0.75rem" : "1rem")
                .set("background-color", "var(--finance-card-bg)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);
        return card;
    }

    private Span createLabelSpan(final String label) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        return labelSpan;
    }

    private Span createValueSpan(final String formattedValue, final boolean compact) {
        Span valueSpan = new Span(formattedValue);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", compact ? "var(--lumo-font-size-xl)" : "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");
        return valueSpan;
    }
}
