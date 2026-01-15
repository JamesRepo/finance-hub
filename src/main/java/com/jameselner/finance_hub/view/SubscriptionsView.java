package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.SubscriptionDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.SubscriptionService;
import com.jameselner.finance_hub.view.components.SubscriptionFormDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Route(value = "subscriptions", layout = MainLayout.class)
@PageTitle("Subscriptions | Finance Hub")
@PermitAll
public class SubscriptionsView extends VerticalLayout {

    private final SubscriptionService subscriptionService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<SubscriptionDTO> grid;
    private SubscriptionFormDialog formDialog;
    private ComboBox<String> filterCombo;
    private ComboBox<Month> monthCombo;
    private ComboBox<Integer> yearCombo;

    private Span monthTotalCard;
    private Span subscriptionCountCard;

    public SubscriptionsView(
            final SubscriptionService subscriptionService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.subscriptionService = subscriptionService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createSummaryCards(),
                createToolbar(),
                createGrid()
        );

        setupFormDialog();
        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Subscriptions");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Add Subscription", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openFormDialog(null));

        Button copyButton = new Button("Copy Last Month", new Icon(VaadinIcon.COPY));
        copyButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        copyButton.addClickListener(event -> copyLastMonth());

        HorizontalLayout header = new HorizontalLayout(title, copyButton, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("mobile-toolbar");

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        monthTotalCard = createSummaryCard("Month Total", "£0.00", "var(--lumo-primary-color)", VaadinIcon.CALENDAR_CLOCK);
        subscriptionCountCard = createSummaryCard("Subscriptions", "0", "var(--lumo-success-color)", VaadinIcon.LIST);

        cardsLayout.add(
                createCardContainer(monthTotalCard),
                createCardContainer(subscriptionCountCard)
        );

        return cardsLayout;
    }

    private HorizontalLayout createToolbar() {
        LocalDate now = LocalDate.now();

        monthCombo = new ComboBox<>("Month");
        monthCombo.setItems(Month.values());
        monthCombo.setItemLabelGenerator(month -> month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        monthCombo.setValue(now.getMonth());
        monthCombo.addValueChangeListener(event -> refreshData());

        yearCombo = new ComboBox<>("Year");
        yearCombo.setItems(IntStream.rangeClosed(now.getYear() - 5, now.getYear() + 1)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList());
        yearCombo.setValue(now.getYear());
        yearCombo.addValueChangeListener(event -> refreshData());

        filterCombo = new ComboBox<>("Frequency");
        filterCombo.setItems("All", "Monthly", "Yearly");
        filterCombo.setValue("All");
        filterCombo.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(monthCombo, yearCombo, filterCombo);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private Grid<SubscriptionDTO> createGrid() {
        grid = new Grid<>(SubscriptionDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("500px");

        grid.addColumn(SubscriptionDTO::getName)
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAmount()))
                .setHeader("Amount")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(SubscriptionDTO::getAmount));

        grid.addColumn(dto -> dto.getFrequency().getDisplayName())
                .setHeader("Frequency")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getMonthlyEquivalent()))
                .setHeader("Monthly")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getAnnualEquivalent()))
                .setHeader("Annual")
                .setAutoWidth(true);

        grid.addColumn(SubscriptionDTO::getPaymentDate)
                .setHeader("Payment Date")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(SubscriptionDTO::getPaymentDate));

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openFormDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> confirmDelete(dto));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private Div createCardContainer(final Span card) {
        Div container = new Div(card);
        container.getStyle()
                .set("flex", "1")
                .set("min-width", "200px");
        return container;
    }

    private Span createSummaryCard(final String label, final String value, final String color, final VaadinIcon icon) {
        Span card = new Span();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("padding", "1rem")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);
        cardIcon.getStyle().set("margin-bottom", "0.5rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, labelSpan, valueSpan);

        return card;
    }

    private void setupFormDialog() {
        formDialog = new SubscriptionFormDialog(subscriptionService);
        formDialog.setSaveListener(subscription -> refreshData());
    }

    private void openFormDialog(final SubscriptionDTO subscription) {
        User currentUser = getCurrentUser();
        formDialog.open(currentUser.getUserId(), subscription);
    }

    private void copyLastMonth() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Copy Last Month's Subscriptions");
        dialog.setText("This will copy all active subscriptions from last month to the current month. Do you want to continue?");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Copy");
        dialog.setConfirmButtonTheme("primary");
        dialog.addConfirmListener(event -> performCopy());

        dialog.open();
    }

    private void performCopy() {
        try {
            User currentUser = getCurrentUser();

            List<SubscriptionDTO> copied = subscriptionService.copyFromPreviousMonth(currentUser.getUserId());

            if (copied.isEmpty()) {
                showInfoNotification("No subscriptions found from last month to copy");
            } else {
                showSuccessNotification("Copied " + copied.size() + " subscription(s) from last month");
                refreshData();
            }
        } catch (Exception e) {
            showErrorNotification("Error copying subscriptions: " + e.getMessage());
        }
    }

    private void confirmDelete(final SubscriptionDTO subscription) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Subscription");
        dialog.setText("Are you sure you want to delete " + subscription.getName() + "? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteSubscription(subscription));

        dialog.open();
    }

    private void deleteSubscription(final SubscriptionDTO subscription) {
        try {
            subscriptionService.deleteById(subscription.getSubscriptionId());
            showSuccessNotification("Subscription deleted successfully");
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting subscription: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshGrid();
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        int year = yearCombo.getValue();
        int month = monthCombo.getValue().getValue();

        BigDecimal monthTotal = subscriptionService.calculateTotalForMonth(userId, year, month);
        long monthCount = subscriptionService.countSubscriptionsForMonth(userId, year, month);

        updateCardValue(monthTotalCard, String.format("£%.2f", monthTotal));
        updateCardValue(subscriptionCountCard, String.valueOf(monthCount));
    }

    private void refreshGrid() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        int year = yearCombo.getValue();
        int month = monthCombo.getValue().getValue();

        String filter = filterCombo.getValue();
        List<SubscriptionDTO> subscriptions = subscriptionService.findByUserIdAndMonthAsDto(userId, year, month);

        subscriptions = switch (filter) {
            case "Monthly" -> subscriptions.stream()
                    .filter(sub -> "MONTHLY".equals(sub.getFrequency().name()))
                    .toList();
            case "Yearly" -> subscriptions.stream()
                    .filter(sub -> "YEARLY".equals(sub.getFrequency().name()))
                    .toList();
            default -> subscriptions;
        };

        grid.setItems(subscriptions);
    }

    private void updateCardValue(final Span card, final String newValue) {
        card.getChildren()
                .skip(2)
                .findFirst()
                .ifPresent(component -> ((Span) component).setText(newValue));
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void showSuccessNotification(final String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showInfoNotification(final String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void showErrorNotification(final String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
