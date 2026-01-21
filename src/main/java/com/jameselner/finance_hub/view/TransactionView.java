package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.AccountService;
import com.jameselner.finance_hub.service.CategoryService;
import com.jameselner.finance_hub.service.TransactionService;
import com.jameselner.finance_hub.view.components.TransactionForm;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Route(value = "transactions", layout = MainLayout.class)
@PageTitle("Transactions | Finance Hub")
@PermitAll
public class TransactionView extends VerticalLayout {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<TransactionDTO> transactionGrid;
    private TransactionForm transactionForm;
    private Dialog formDialog;
    private ComboBox<YearMonth> monthFilter;
    private ComboBox<Category> categoryFilter;
    private TextField searchField;
    private YearMonth selectedMonth;
    private Category selectedCategory;
    private String searchTerm;
    private Span totalSpentLabel;

    public TransactionView(
            final TransactionService transactionService,
            final AccountService accountService,
            final CategoryService categoryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createGrid();
        createFormDialog();
        createSearchField();
        createMonthFilter();
        createCategoryTypeFilter();

        add(createSummarySection(), createToolbar(), transactionGrid);
        refreshGrid();
    }

    private void createHeader() {
        H2 header = new H2("Transactions");
        add(header);
    }

    private HorizontalLayout createSummarySection() {
        totalSpentLabel = new Span();
        totalSpentLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "500");

        HorizontalLayout summary = new HorizontalLayout();
        summary.setWidthFull();
        summary.add(new Span("Total Spent: "), totalSpentLabel);
        summary.setAlignItems(FlexComponent.Alignment.CENTER);
        summary.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        return summary;
    }

    private void updateSummary() {
        if (selectedMonth != null) {
            User currentUser = getCurrentUser();
            LocalDate startDate = selectedMonth.atDay(1);
            LocalDate endDate = selectedMonth.atEndOfMonth();

            BigDecimal totalSpent = transactionService.sumByUserAndTypeAndDateRange(
                    currentUser,
                    com.jameselner.finance_hub.domain.enums.TransactionType.EXPENSE,
                    startDate,
                    endDate
            );

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
            totalSpentLabel.setText(currencyFormat.format(totalSpent));
        } else {
            totalSpentLabel.setText("-");
        }
    }

    private HorizontalLayout createToolbar() {
        Button addButton = new Button("Add Transaction");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> openFormForNew());

        HorizontalLayout leftSection = new HorizontalLayout(searchField, monthFilter, categoryFilter);
        leftSection.setSpacing(true);
        leftSection.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSection.addClassName("mobile-stack");

        HorizontalLayout toolbar = new HorizontalLayout(leftSection, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.addClassName("mobile-toolbar");

        return toolbar;
    }

    private void createGrid() {
        transactionGrid = new Grid<>(TransactionDTO.class, false);
        transactionGrid.setSizeFull();

        transactionGrid.addColumn(TransactionDTO::getTransactionDate)
                .setHeader("Date")
                .setSortable(true);

        transactionGrid.addColumn(new ComponentRenderer<>(this::createCategoryBadge))
                .setHeader("Category")
                .setAutoWidth(true);

        transactionGrid.addColumn(TransactionDTO::getPlaceVenue)
                .setHeader("Place/Venue")
                .setAutoWidth(true);

        transactionGrid.addColumn(dto -> "£" + dto.getAmount())
                .setHeader("Amount")
                .setAutoWidth(true);

        transactionGrid.addColumn(TransactionDTO::getDescription)
                .setHeader("Description")
                .setAutoWidth(true);

        transactionGrid.addColumn(TransactionDTO::getTransactionType)
                .setHeader("Type")
                .setAutoWidth(true);

        transactionGrid.addColumn(TransactionDTO::getAccountName)
                .setHeader("Account")
                .setAutoWidth(true);

        transactionGrid.addColumn(new ComponentRenderer<>(this::createDeleteButton))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        transactionGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openFormForEdit(event.getValue());
            }
        });
    }

    private void createSearchField() {
        searchField = new TextField();
        searchField.setPlaceholder("Search all transactions...");
        searchField.setWidth("250px");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);

        searchField.addValueChangeListener(event -> {
            searchTerm = event.getValue();
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Clear other filters when searching
                monthFilter.clear();
                categoryFilter.clear();
                selectedMonth = null;
                selectedCategory = null;
            }
            refreshGrid();
        });
    }

    private void createFormDialog() {
        formDialog = new Dialog();
        formDialog.setHeaderTitle("Transaction");
        formDialog.setWidth("min(800px, 95vw)");
        formDialog.setMaxHeight("90vh");

        transactionForm = new TransactionForm(transactionService, accountService, categoryService);

        // Set default account for the current user
        User currentUser = getCurrentUser();
        accountService.findDefaultByUserId(currentUser.getUserId())
                .ifPresent(transactionForm::setDefaultAccount);

        transactionForm.setSaveListener(transaction -> {
            formDialog.close();
            refreshGrid();
        });

        transactionForm.setCancelListener(() -> formDialog.close());

        formDialog.add(transactionForm);
    }

    private User getCurrentUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void openFormForNew() {
        transactionForm.clearForm();
        formDialog.setHeaderTitle("Add Transaction");
        formDialog.open();
    }

    private void openFormForEdit(TransactionDTO transaction) {
        transactionForm.setTransaction(transaction);
        formDialog.setHeaderTitle("Edit Transaction");
        formDialog.open();
    }

    private void createMonthFilter() {
        monthFilter = new ComboBox<>();
        monthFilter.setWidth("200px");
        monthFilter.setPlaceholder("Filter by Month");

        // Create list of last 24 months
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 24; i++) {
            months.add(current.minusMonths(i));
        }

        monthFilter.setItems(months);
        monthFilter.setItemLabelGenerator(month ->
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Set default to current month
        selectedMonth = YearMonth.now();
        monthFilter.setValue(selectedMonth);

        // Add listener to refresh grid when selection changes
        monthFilter.addValueChangeListener(event -> {
            selectedMonth = event.getValue();
            if (selectedMonth != null) {
                // Clear search when selecting a month
                searchField.clear();
                searchTerm = null;
            }
            refreshGrid();
        });
    }

    private void createCategoryTypeFilter() {
        categoryFilter = new ComboBox<>();
        categoryFilter.setWidth("180px");
        categoryFilter.setPlaceholder("Filter by Category");

        List<Category> categories = categoryService.findAll();
        categoryFilter.setItems(categories);
        categoryFilter.setItemLabelGenerator(Category::getCategoryName);
        categoryFilter.setClearButtonVisible(true);

        // No default selection (shows all)
        selectedCategory = null;

        // Add listener to refresh grid when selection changes
        categoryFilter.addValueChangeListener(event -> {
            selectedCategory = event.getValue();
            if (selectedCategory != null) {
                // Clear search when selecting a category
                searchField.clear();
                searchTerm = null;
            }
            refreshGrid();
        });
    }

    private void refreshGrid() {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Search across all transactions
            transactionGrid.setItems(transactionService.searchByTermAsDto(searchTerm));
        } else if (selectedMonth != null && selectedCategory != null) {
            // Filter by both month and category type
            transactionGrid.setItems(transactionService.findByYearAndMonthAndCategoryAsDto(
                selectedMonth.getYear(),
                selectedMonth.getMonthValue(),
                selectedCategory
            ));
        } else if (selectedMonth != null) {
            // Filter by month only
            transactionGrid.setItems(transactionService.findByYearAndMonthAsDto(
                selectedMonth.getYear(),
                selectedMonth.getMonthValue()
            ));
        } else {
            // No filters - default to current month
            YearMonth current = YearMonth.now();
            transactionGrid.setItems(transactionService.findByYearAndMonthAsDto(
                current.getYear(),
                current.getMonthValue()
            ));
        }
        updateSummary();
    }

    private HorizontalLayout createCategoryBadge(TransactionDTO transaction) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setPadding(false);

        // Create colored indicator dot
        Div colorDot = new Div();
        colorDot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", transaction.getCategoryColorCode() != null ?
                        transaction.getCategoryColorCode() : "var(--finance-text-secondary)")
                .set("flex-shrink", "0");

        Span categoryName = new Span(transaction.getCategoryName());

        layout.add(colorDot, categoryName);
        return layout;
    }

    private Button createDeleteButton(final TransactionDTO transaction) {
        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClassName("mobile-touch-target");
        deleteButton.getElement().setAttribute("aria-label", "Delete transaction");

        deleteButton.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.access(() -> showDeleteConfirmation(transaction))));

        return deleteButton;
    }

    private void showDeleteConfirmation(final TransactionDTO transaction) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Transaction");
        dialog.setText(
            String.format("""
                            Are you sure you want to delete this transaction?
                            
                            Date: %s
                            Category: %s
                            Amount: £%s
                            Place: %s""",
                transaction.getTransactionDate(),
                transaction.getCategoryName(),
                transaction.getAmount(),
                transaction.getPlaceVenue() != null ? transaction.getPlaceVenue() : "N/A")
        );

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deleteTransaction(transaction));

        dialog.open();
    }

    private void deleteTransaction(final TransactionDTO transaction) {
        transactionService.deleteById(transaction.getTransactionId());
        refreshGrid();
    }
}