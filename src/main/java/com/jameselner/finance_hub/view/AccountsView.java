package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.AccountDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.AccountService;
import com.jameselner.finance_hub.view.components.AccountFormDialog;
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
import java.util.List;

@Route(value = "accounts", layout = MainLayout.class)
@PageTitle("Accounts | Finance Hub")
@PermitAll
public class AccountsView extends VerticalLayout {

    private final AccountService accountService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<AccountDTO> grid;
    private AccountFormDialog formDialog;
    private ComboBox<String> filterCombo;

    private Span totalBalanceCard;
    private Span accountCountCard;

    public AccountsView(
            final AccountService accountService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.accountService = accountService;
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
        H2 title = new H2("Accounts");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Add Account", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openFormDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, addButton);
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

        totalBalanceCard = createSummaryCard("Total Balance", "£0.00", "var(--lumo-success-color)", VaadinIcon.MONEY);
        accountCountCard = createSummaryCard("Total Accounts", "0", "var(--lumo-primary-color)", VaadinIcon.BRIEFCASE);

        cardsLayout.add(
                createCardContainer(totalBalanceCard),
                createCardContainer(accountCountCard)
        );

        return cardsLayout;
    }

    private HorizontalLayout createToolbar() {
        filterCombo = new ComboBox<>("Filter by");
        filterCombo.setItems("All", "Active", "Inactive");
        filterCombo.setValue("Active");
        filterCombo.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(filterCombo);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private Grid<AccountDTO> createGrid() {
        grid = new Grid<>(AccountDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("500px");

        grid.addColumn(AccountDTO::getAccountName)
                .setHeader("Account Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> {
            return switch (dto.getAccountType()) {
                case CURRENT -> "Current Account";
                case SAVINGS -> "Savings Account";
                case CREDIT_CARD -> "Credit Card";
                case INVESTMENT -> "Investment Account";
                case CASH -> "Cash";
                case IMPORTED -> "Imported Account";
                case OTHER -> "Other";
            };
        })
                .setHeader("Type")
                .setAutoWidth(true);

        grid.addColumn(dto -> String.format("£%.2f", dto.getBalance()))
                .setHeader("Balance")
                .setAutoWidth(true);

        grid.addColumn(AccountDTO::getCurrency)
                .setHeader("Currency")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Span badge = new Span(dto.getIsActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(dto.getIsActive() ? "success" : "contrast");
            return badge;
        })
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editButton.addClassName("mobile-touch-target");
            editButton.getElement().setAttribute("aria-label", "Edit account");
            editButton.addClickListener(event -> openFormDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.addClassName("mobile-touch-target");
            deleteButton.getElement().setAttribute("aria-label", "Delete account");
            deleteButton.addClickListener(event -> confirmDelete(dto));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            actions.addClassName("action-buttons");
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
        formDialog = new AccountFormDialog(accountService);
        formDialog.setSaveListener(account -> refreshData());
    }

    private void openFormDialog(final AccountDTO account) {
        User currentUser = getCurrentUser();
        formDialog.open(currentUser.getUserId(), account);
    }

    private void confirmDelete(final AccountDTO account) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Account");
        dialog.setText("Are you sure you want to delete " + account.getAccountName() + "? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteAccount(account));

        dialog.open();
    }

    private void deleteAccount(final AccountDTO account) {
        try {
            accountService.deleteById(account.getAccountId());
            showSuccessNotification("Account deleted successfully");
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting account: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshGrid();
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        List<AccountDTO> activeAccounts = accountService.findActiveByUserIdAsDto(userId);
        BigDecimal totalBalance = activeAccounts.stream()
                .map(AccountDTO::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long accountCount = accountService.countByUserId(userId);

        updateCardValue(totalBalanceCard, String.format("£%.2f", totalBalance));
        updateCardValue(accountCountCard, String.valueOf(accountCount));
    }

    private void refreshGrid() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        String filter = filterCombo.getValue();
        List<AccountDTO> accounts;

        accounts = switch (filter) {
            case "Active" -> accountService.findActiveByUserIdAsDto(userId);
            case "Inactive" -> accountService.findByUserIdAsDto(userId).stream()
                    .filter(acc -> !acc.getIsActive())
                    .toList();
            default -> accountService.findByUserIdAsDto(userId);
        };

        grid.setItems(accounts);
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

    private void showErrorNotification(final String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
