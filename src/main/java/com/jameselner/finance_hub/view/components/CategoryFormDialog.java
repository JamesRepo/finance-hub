package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.dto.CategoryDTO;
import com.jameselner.finance_hub.service.CategoryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import lombok.Setter;

import java.util.Objects;
import java.util.function.Consumer;

public class CategoryFormDialog extends Dialog {

    private final CategoryService categoryService;
    private final Binder<CategoryDTO> binder = new BeanValidationBinder<>(CategoryDTO.class);

    private Long userId;
    private CategoryDTO currentCategory;

    private TextField categoryNameField;
    private ComboBox<CategoryType> categoryTypeCombo;
    private TextField colorCodeField;

    @Setter
    private Consumer<CategoryDTO> saveListener;

    public CategoryFormDialog(final CategoryService categoryService) {
        this.categoryService = categoryService;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setModal(true);
        setWidth("500px");

        createFormFields();
        setupBinder();
    }

    public void open(final Long userId, final CategoryDTO category) {
        this.userId = userId;
        this.currentCategory = category;

        removeAll();

        H3 title = new H3(category != null && category.getCategoryId() != null ?
                "Edit Category" : "Add Category");
        title.getStyle().set("margin", "0");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout mainLayout = new VerticalLayout(title, formLayout, buttonLayout);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        add(mainLayout);

        if (category != null) {
            // Check if it's a system category
            if (category.getIsSystem() != null && category.getIsSystem()) {
                showErrorNotification("System categories cannot be edited");
                close();
                return;
            }
            binder.readBean(category);
        } else {
            clearForm();
        }

        open();
    }

    private void createFormFields() {
        categoryNameField = new TextField("Category Name");
        categoryNameField.setRequiredIndicatorVisible(true);
        categoryNameField.setHelperText("e.g., Groceries, Entertainment, Utilities");
        categoryNameField.setPlaceholder("Enter category name");

        categoryTypeCombo = new ComboBox<>("Category Type");
        categoryTypeCombo.setItems(CategoryType.values());
        categoryTypeCombo.setItemLabelGenerator(type -> type == CategoryType.INCOME ? "Income" : "Expense");
        categoryTypeCombo.setRequiredIndicatorVisible(true);
        categoryTypeCombo.setHelperText("Select whether this is income or expense");

        colorCodeField = new TextField("Color Code");
        colorCodeField.setHelperText("Optional: Hex color code (e.g., #FF5733)");
        colorCodeField.setPlaceholder("#000000");
        colorCodeField.setMaxLength(7);
    }

    private FormLayout createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(categoryNameField);
        formLayout.setColspan(categoryNameField, 2);

        formLayout.add(
                categoryTypeCombo,
                colorCodeField
        );

        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveCategory());

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setPadding(true);

        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(categoryNameField)
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Category name is required")
                .bind(CategoryDTO::getCategoryName, CategoryDTO::setCategoryName);

        binder.forField(categoryTypeCombo)
                .withValidator(Objects::nonNull, "Category type is required")
                .bind(CategoryDTO::getCategoryType, CategoryDTO::setCategoryType);

        binder.forField(colorCodeField)
                .withValidator(color -> color == null || color.isEmpty() ||
                        (color.matches("#[0-9A-Fa-f]{6}")),
                        "Color must be in hex format (e.g., #FF5733)")
                .bind(CategoryDTO::getColorCode, CategoryDTO::setColorCode);
    }

    private void saveCategory() {
        try {
            CategoryDTO dto = currentCategory != null ? currentCategory : new CategoryDTO();
            dto.setUserId(userId);

            binder.writeBean(dto);

            CategoryDTO savedDto;
            if (dto.getCategoryId() == null) {
                savedDto = categoryService.createFromDto(dto);
                showSuccessNotification("Category added successfully");
            } else {
                savedDto = categoryService.updateFromDto(dto);
                showSuccessNotification("Category updated successfully");
            }

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

            close();

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving category: " + e.getMessage());
        }
    }

    private void clearForm() {
        currentCategory = null;
        CategoryDTO emptyDto = new CategoryDTO();
        binder.readBean(emptyDto);
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
