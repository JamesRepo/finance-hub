package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.dto.CategoryDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.CategoryService;
import com.jameselner.finance_hub.view.components.CategoryFormDialog;
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

import java.util.List;

@Route(value = "categories", layout = MainLayout.class)
@PageTitle("Categories | Finance Hub")
@PermitAll
public class CategoriesView extends VerticalLayout {

    private final CategoryService categoryService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<CategoryDTO> grid;
    private CategoryFormDialog formDialog;
    private ComboBox<String> filterCombo;

    private Span incomeCountCard;
    private Span expenseCountCard;

    public CategoriesView(
            final CategoryService categoryService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.categoryService = categoryService;
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
        H2 title = new H2("Categories");
        title.getStyle().set("margin", "0");

        Button addButton = new Button("Add Category", new Icon(VaadinIcon.PLUS));
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

        incomeCountCard = createSummaryCard("Income Categories", "0", "var(--lumo-success-color)", VaadinIcon.ARROW_DOWN);
        expenseCountCard = createSummaryCard("Expense Categories", "0", "var(--lumo-error-color)", VaadinIcon.ARROW_UP);

        cardsLayout.add(
                createCardContainer(incomeCountCard),
                createCardContainer(expenseCountCard)
        );

        return cardsLayout;
    }

    private HorizontalLayout createToolbar() {
        filterCombo = new ComboBox<>("Filter by");
        filterCombo.setItems("All", "Income", "Expense");
        filterCombo.setValue("All");
        filterCombo.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout toolbar = new HorizontalLayout(filterCombo);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        return toolbar;
    }

    private Grid<CategoryDTO> createGrid() {
        grid = new Grid<>(CategoryDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("500px");

        grid.addColumn(CategoryDTO::getCategoryName)
                .setHeader("Category Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(dto -> {
            Span badge = new Span(dto.getCategoryType() == CategoryType.INCOME ? "Income" : "Expense");
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(dto.getCategoryType() == CategoryType.INCOME ? "success" : "error");
            return badge;
        })
                .setHeader("Type")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            if (dto.getColorCode() != null && !dto.getColorCode().isEmpty()) {
                Div colorBox = new Div();
                colorBox.getStyle()
                        .set("width", "30px")
                        .set("height", "20px")
                        .set("background-color", dto.getColorCode())
                        .set("border", "1px solid var(--lumo-contrast-20pct)")
                        .set("border-radius", "var(--lumo-border-radius-s)");
                return colorBox;
            }
            return new Span("-");
        })
                .setHeader("Color")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            if (dto.getIsSystem()) {
                Span badge = new Span("System");
                badge.getElement().getThemeList().add("badge");
                badge.getElement().getThemeList().add("contrast");
                return badge;
            }
            return new Span("Custom");
        })
                .setHeader("Source")
                .setAutoWidth(true);

        grid.addComponentColumn(dto -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openFormDialog(dto));
            editButton.setEnabled(!dto.getIsSystem());

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> confirmDelete(dto));
            deleteButton.setEnabled(!dto.getIsSystem());

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
        formDialog = new CategoryFormDialog(categoryService);
        formDialog.setSaveListener(category -> refreshData());
    }

    private void openFormDialog(final CategoryDTO category) {
        User currentUser = getCurrentUser();
        formDialog.open(currentUser.getUserId(), category);
    }

    private void confirmDelete(final CategoryDTO category) {
        if (category.getIsSystem()) {
            showErrorNotification("System categories cannot be deleted");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Category");
        dialog.setText("Are you sure you want to delete " + category.getCategoryName() + "? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteCategory(category));

        dialog.open();
    }

    private void deleteCategory(final CategoryDTO category) {
        try {
            categoryService.deleteById(category.getCategoryId());
            showSuccessNotification("Category deleted successfully");
            refreshData();
        } catch (Exception e) {
            showErrorNotification("Error deleting category: " + e.getMessage());
        }
    }

    private void refreshData() {
        refreshSummaryCards();
        refreshGrid();
    }

    private void refreshSummaryCards() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        long incomeCount = categoryService.findByUserIdAndTypeAsDto(userId, CategoryType.INCOME).size();
        long expenseCount = categoryService.findByUserIdAndTypeAsDto(userId, CategoryType.EXPENSE).size();

        updateCardValue(incomeCountCard, String.valueOf(incomeCount));
        updateCardValue(expenseCountCard, String.valueOf(expenseCount));
    }

    private void refreshGrid() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();

        String filter = filterCombo.getValue();
        List<CategoryDTO> categories;

        categories = switch (filter) {
            case "Income" -> categoryService.findByUserIdAndTypeAsDto(userId, CategoryType.INCOME);
            case "Expense" -> categoryService.findByUserIdAndTypeAsDto(userId, CategoryType.EXPENSE);
            default -> categoryService.findByUserIdAsDto(userId);
        };

        grid.setItems(categories);
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
