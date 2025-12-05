package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.CategoryType;
import com.jameselner.finance_hub.domain.enums.RecurrenceFrequency;
import com.jameselner.finance_hub.dto.IncomeDeductionDTO;
import com.jameselner.finance_hub.dto.IncomeSourceDTO;
import com.jameselner.finance_hub.repository.CategoryRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.IncomeDeductionService;
import com.jameselner.finance_hub.service.IncomeSourceService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.BigDecimalRangeValidator;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IncomeSourceForm extends VerticalLayout {

    private final IncomeSourceService incomeSourceService;
    private final IncomeDeductionService incomeDeductionService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final Binder<IncomeSourceDTO> binder = new BeanValidationBinder<>(IncomeSourceDTO.class);
    private IncomeSourceDTO currentIncomeSource;
    private final List<IncomeDeductionDTO> deductions = new ArrayList<>();

    private TextField sourceNameField;
    private TextArea descriptionField;
    private BigDecimalField grossAmountField;
    private Span netAmountDisplay;
    private Checkbox isRecurringCheckbox;
    private ComboBox<RecurrenceFrequency> recurrenceFrequencyCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private ComboBox<Category> categoryCombo;
    private Checkbox isActiveCheckbox;
    private Checkbox autoCreateTransactionCheckbox;

    private Grid<IncomeDeductionDTO> deductionsGrid;
    private DeductionFormDialog deductionDialog;
    private VerticalLayout deductionsSection;

    private Button saveButton;
    private Button cancelButton;

    @Setter
    private Consumer<IncomeSourceDTO> saveListener;
    @Setter
    private Runnable cancelListener;

    public IncomeSourceForm(
            final IncomeSourceService incomeSourceService,
            final IncomeDeductionService incomeDeductionService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository,
            final CategoryRepository categoryRepository
    ) {
        this.incomeSourceService = incomeSourceService;
        this.incomeDeductionService = incomeDeductionService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;

        addClassName("income-source-form");
        setSpacing(true);
        setPadding(true);

        createFormFields();
        createDeductionsSection();
        createButtons();
        setupDeductionDialog();

        add(
                createFormLayout(),
                createNetAmountSection(),
                deductionsSection,
                createButtonLayout()
        );

        setupBinder();
        setupFieldListeners();

        // Initialize grid with empty list
        refreshDeductionsGrid();
    }

    private void createFormFields() {
        sourceNameField = new TextField("Income Source Name");
        sourceNameField.setRequiredIndicatorVisible(true);
        sourceNameField.setHelperText("e.g., Full-time Salary, Freelance Work");
        sourceNameField.setPlaceholder("Enter source name");

        descriptionField = new TextArea("Description");
        descriptionField.setHelperText("Optional details about this income source");
        descriptionField.setMaxLength(1000);

        grossAmountField = new BigDecimalField("Gross Amount (Pre-Tax)");
        grossAmountField.setPrefixComponent(new Span("£"));
        grossAmountField.setRequiredIndicatorVisible(true);
        grossAmountField.setHelperText("Your salary before any deductions");
        grossAmountField.addValueChangeListener(event -> updateNetAmount());

        isRecurringCheckbox = new Checkbox("Recurring Income");
        isRecurringCheckbox.setHelperText("Check if this income repeats regularly");

        recurrenceFrequencyCombo = new ComboBox<>("Frequency");
        recurrenceFrequencyCombo.setItems(
                RecurrenceFrequency.WEEKLY,
                RecurrenceFrequency.BI_WEEKLY,
                RecurrenceFrequency.MONTHLY,
                RecurrenceFrequency.QUARTERLY,
                RecurrenceFrequency.SEMI_ANNUALLY,
                RecurrenceFrequency.ANNUALLY
        );
        recurrenceFrequencyCombo.setItemLabelGenerator(RecurrenceFrequency::getDisplayName);
        recurrenceFrequencyCombo.setHelperText("How often does this income occur?");
        recurrenceFrequencyCombo.setEnabled(false);

        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setRequiredIndicatorVisible(true);
        startDatePicker.setValue(LocalDate.now());
        startDatePicker.setHelperText("When did/will this income start?");

        endDatePicker = new DatePicker("End Date");
        endDatePicker.setHelperText("Optional: When will this income end?");

        List<Category> incomeCategories = categoryRepository.findByCategoryType(CategoryType.INCOME);
        categoryCombo = new ComboBox<>("Category");
        categoryCombo.setItems(incomeCategories);
        categoryCombo.setItemLabelGenerator(Category::getCategoryName);
        categoryCombo.setHelperText("Optional: Categorize this income");

        isActiveCheckbox = new Checkbox("Active");
        isActiveCheckbox.setValue(true);
        isActiveCheckbox.setHelperText("Inactive sources won't be included in forecasts");

        autoCreateTransactionCheckbox = new Checkbox("Auto-create Transactions");
        autoCreateTransactionCheckbox.setValue(false);
        autoCreateTransactionCheckbox.setHelperText("Automatically create transaction when income is due");
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(sourceNameField);
        formLayout.setColspan(sourceNameField, 2);

        formLayout.add(descriptionField);
        formLayout.setColspan(descriptionField, 2);

        formLayout.add(
                grossAmountField,
                categoryCombo,
                isRecurringCheckbox,
                recurrenceFrequencyCombo,
                startDatePicker,
                endDatePicker,
                isActiveCheckbox,
                autoCreateTransactionCheckbox
        );

        return formLayout;
    }

    private Component createNetAmountSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "1rem");

        Span label = new Span("Net Take-Home (After Deductions):");
        label.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        netAmountDisplay = new Span("£0.00");
        netAmountDisplay.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-success-color)");

        section.add(label, netAmountDisplay);

        return section;
    }

    private void createDeductionsSection() {
        deductionsSection = new VerticalLayout();
        deductionsSection.setPadding(false);
        deductionsSection.setSpacing(true);
        deductionsSection.getStyle().set("margin-top", "1rem");

        H4 sectionTitle = new H4("Deductions");
        sectionTitle.getStyle().set("margin", "0");

        Button addDeductionButton = new Button("Add Deduction", new Icon(VaadinIcon.PLUS));
        addDeductionButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addDeductionButton.addClickListener(event -> openDeductionDialog(null));

        HorizontalLayout header = new HorizontalLayout(sectionTitle, addDeductionButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        deductionsGrid = createDeductionsGrid();

        Div gridContainer = new Div(deductionsGrid);
        gridContainer.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "hidden");

        deductionsSection.add(header, gridContainer);
    }

    private Grid<IncomeDeductionDTO> createDeductionsGrid() {
        Grid<IncomeDeductionDTO> grid = new Grid<>(IncomeDeductionDTO.class, false);
        grid.setAllRowsVisible(true);
        grid.setMaxHeight("300px");

        grid.addColumn(dto -> dto.getDeductionType().getDisplayName())
                .setHeader("Type")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(IncomeDeductionDTO::getDeductionName)
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(dto -> {
            if (dto.getIsPercentage()) {
                return String.format("%.2f%%", dto.getPercentageValue());
            } else {
                return String.format("£%.2f", dto.getAmount());
            }
        })
                .setHeader("Amount")
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
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(event -> openDeductionDialog(dto));

            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> deleteDeduction(dto));

            HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private void setupDeductionDialog() {
        deductionDialog = new DeductionFormDialog(incomeDeductionService);
        deductionDialog.setSaveListener(deduction -> {
            // Update or add deduction in the list
            boolean found = false;
            for (int i = 0; i < deductions.size(); i++) {
                if (deductions.get(i).getDeductionId() != null &&
                        deductions.get(i).getDeductionId().equals(deduction.getDeductionId())) {
                    deductions.set(i, deduction);
                    found = true;
                    break;
                }
            }
            if (!found) {
                deductions.add(deduction);
            }
            refreshDeductionsGrid();
            updateNetAmount();
        });
    }

    private void openDeductionDialog(final IncomeDeductionDTO deduction) {
        // Allow adding deductions even before saving the income source
        Long incomeSourceId = (currentIncomeSource != null) ? currentIncomeSource.getIncomeSourceId() : null;
        deductionDialog.open(incomeSourceId, deduction);
    }

    private void deleteDeduction(final IncomeDeductionDTO deduction) {
        if (deduction.getDeductionId() != null) {
            try {
                incomeDeductionService.deleteById(deduction.getDeductionId());
                deductions.remove(deduction);
                refreshDeductionsGrid();
                updateNetAmount();
                showSuccessNotification("Deduction deleted successfully");
            } catch (Exception e) {
                showErrorNotification("Error deleting deduction: " + e.getMessage());
            }
        } else {
            deductions.remove(deduction);
            refreshDeductionsGrid();
            updateNetAmount();
        }
    }

    private void refreshDeductionsGrid() {
        deductionsGrid.setItems(deductions);
    }

    private void updateNetAmount() {
        BigDecimal grossAmountValue = grossAmountField.getValue();
        if (grossAmountValue == null) {
            grossAmountValue = BigDecimal.ZERO;
        }

        final BigDecimal grossAmount = grossAmountValue;

        BigDecimal totalDeductions = deductions.stream()
                .filter(IncomeDeductionDTO::getIsActive)
                .map(dto -> {
                    if (dto.getIsPercentage() && dto.getPercentageValue() != null) {
                        return grossAmount.multiply(dto.getPercentageValue())
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    } else if (dto.getAmount() != null) {
                        return dto.getAmount();
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = grossAmount.subtract(totalDeductions);
        netAmountDisplay.setText(String.format("£%.2f", netAmount));

        // Also update the DTO if it exists
        if (currentIncomeSource != null) {
            currentIncomeSource.setNetAmount(netAmount);
        }
    }

    private void createButtons() {
        saveButton = new Button("Save Income Source");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> saveIncomeSource());

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(event -> cancel());
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        return buttonLayout;
    }

    private void setupBinder() {
        binder.forField(sourceNameField)
                .withValidator(Objects::nonNull, "Source name is required")
                .withValidator(name -> !name.trim().isEmpty(), "Source name cannot be empty")
                .bind(IncomeSourceDTO::getSourceName, IncomeSourceDTO::setSourceName);

        binder.forField(descriptionField)
                .bind(IncomeSourceDTO::getDescription, IncomeSourceDTO::setDescription);

        binder.forField(grossAmountField)
                .withValidator(new BigDecimalRangeValidator(
                        "Amount must be greater than zero",
                        new BigDecimal("0.01"),
                        new BigDecimal("999999999.99")))
                .bind(IncomeSourceDTO::getGrossAmount, IncomeSourceDTO::setGrossAmount);

        binder.forField(isRecurringCheckbox)
                .bind(IncomeSourceDTO::getIsRecurring, IncomeSourceDTO::setIsRecurring);

        binder.forField(recurrenceFrequencyCombo)
                .bind(IncomeSourceDTO::getRecurrenceFrequency, IncomeSourceDTO::setRecurrenceFrequency);

        binder.forField(startDatePicker)
                .withValidator(Objects::nonNull, "Start date is required")
                .bind(IncomeSourceDTO::getStartDate, IncomeSourceDTO::setStartDate);

        binder.forField(endDatePicker)
                .bind(IncomeSourceDTO::getEndDate, IncomeSourceDTO::setEndDate);

        binder.forField(categoryCombo)
                .bind(
                        dto -> {
                            if (dto.getCategoryId() != null) {
                                return categoryRepository.findById(dto.getCategoryId()).orElse(null);
                            }
                            return null;
                        },
                        (dto, category) -> {
                            if (category != null) {
                                dto.setCategoryId(category.getCategoryId());
                                dto.setCategoryName(category.getCategoryName());
                            } else {
                                dto.setCategoryId(null);
                                dto.setCategoryName(null);
                            }
                        }
                );

        binder.forField(isActiveCheckbox)
                .bind(IncomeSourceDTO::getIsActive, IncomeSourceDTO::setIsActive);

        binder.forField(autoCreateTransactionCheckbox)
                .bind(IncomeSourceDTO::getAutoCreateTransaction, IncomeSourceDTO::setAutoCreateTransaction);
    }

    private void setupFieldListeners() {
        isRecurringCheckbox.addValueChangeListener(event -> {
            boolean isRecurring = event.getValue();
            recurrenceFrequencyCombo.setEnabled(isRecurring);
            recurrenceFrequencyCombo.setRequiredIndicatorVisible(isRecurring);

            if (!isRecurring) {
                recurrenceFrequencyCombo.clear();
            }
        });
    }

    private void saveIncomeSource() {
        try {
            IncomeSourceDTO dto = currentIncomeSource != null ? currentIncomeSource : new IncomeSourceDTO();

            User currentUser = getCurrentUser();
            dto.setUserId(currentUser.getUserId());

            binder.writeBean(dto);

            // Set amount field to gross amount for backward compatibility
            dto.setAmount(dto.getGrossAmount());

            // Update net amount
            updateNetAmount();
            dto.setNetAmount(currentIncomeSource != null ? currentIncomeSource.getNetAmount() : BigDecimal.ZERO);

            IncomeSourceDTO savedDto;
            boolean isNewIncomeSource = dto.getIncomeSourceId() == null;

            if (isNewIncomeSource) {
                savedDto = incomeSourceService.createIncomeSourceFromDto(dto);

                // Save any deductions that were added before the income source was saved
                if (!deductions.isEmpty()) {
                    for (int i = 0; i < deductions.size(); i++) {
                        IncomeDeductionDTO deduction = deductions.get(i);
                        if (deduction.getDeductionId() == null) {
                            // This is an unsaved deduction - save it now with the income source ID
                            deduction.setIncomeSourceId(savedDto.getIncomeSourceId());
                            IncomeDeductionDTO savedDeduction = incomeDeductionService.createDeductionFromDto(deduction);
                            deductions.set(i, savedDeduction);
                        }
                    }
                }

                showSuccessNotification("Income source created successfully");
            } else {
                savedDto = incomeSourceService.updateIncomeSourceFromDto(dto);
                showSuccessNotification("Income source updated successfully");
            }

            // Update the current income source reference
            currentIncomeSource = savedDto;

            if (saveListener != null) {
                saveListener.accept(savedDto);
            }

        } catch (ValidationException e) {
            showErrorNotification("Please check the form for errors");
        } catch (Exception e) {
            showErrorNotification("Error saving income source: " + e.getMessage());
        }
    }

    private void cancel() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    public void setIncomeSource(final IncomeSourceDTO dto) {
        this.currentIncomeSource = dto;
        binder.readBean(dto);

        if (dto.getIsRecurring() != null && dto.getIsRecurring()) {
            recurrenceFrequencyCombo.setEnabled(true);
        }

        // Load deductions for this income source
        if (dto.getIncomeSourceId() != null) {
            loadDeductions(dto.getIncomeSourceId());
        }
    }

    private void loadDeductions(final Long incomeSourceId) {
        deductions.clear();
        deductions.addAll(incomeDeductionService.findByIncomeSourceAsDto(incomeSourceId));
        refreshDeductionsGrid();
        updateNetAmount();
    }

    public void clearForm() {
        this.currentIncomeSource = null;
        deductions.clear();
        IncomeSourceDTO emptyDto = new IncomeSourceDTO();
        emptyDto.setIsRecurring(false);
        emptyDto.setIsActive(true);
        emptyDto.setAutoCreateTransaction(false);
        emptyDto.setStartDate(LocalDate.now());
        binder.readBean(emptyDto);
        recurrenceFrequencyCombo.setEnabled(false);
        refreshDeductionsGrid();
        updateNetAmount();
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
