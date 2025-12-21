package com.jameselner.finance_hub.view;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.Priority;
import com.jameselner.finance_hub.dto.SavingsGoalDTO;
import com.jameselner.finance_hub.dto.SavingsGoalProgressDTO;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.service.SavingsGoalContributionService;
import com.jameselner.finance_hub.service.SavingsGoalService;
import com.jameselner.finance_hub.view.components.SavingsGoalContributionDialog;
import com.jameselner.finance_hub.view.components.SavingsGoalForm;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "savings-goals", layout = MainLayout.class)
@PageTitle("Savings Goals | Finance Hub")
@PermitAll
public class SavingsGoalsView extends VerticalLayout {

    private final SavingsGoalService savingsGoalService;
    private final SavingsGoalContributionService contributionService;
    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    private Grid<SavingsGoalProgressDTO> grid;
    private ComboBox<String> filterComboBox;

    private Span totalTargetAmountSpan;
    private Span totalSavedAmountSpan;
    private Span goalsCountSpan;
    private Span completionRateSpan;
    private Span recommendedContributionSpan;

    private SavingsGoalForm savingsGoalForm;
    private Dialog formDialog;
    private SavingsGoalContributionDialog contributionDialog;

    public SavingsGoalsView(
            final SavingsGoalService savingsGoalService,
            final SavingsGoalContributionService contributionService,
            final AuthenticationContext authenticationContext,
            final UserRepository userRepository
    ) {
        this.savingsGoalService = savingsGoalService;
        this.contributionService = contributionService;
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;

        addClassName("savings-goals-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createSummaryCards(),
                createToolbar(),
                createGrid()
        );

        setupDialogs();
        refreshData();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Savings Goals");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        return header;
    }

    private HorizontalLayout createSummaryCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.addClassName("mobile-stack");

        cardsLayout.add(
                createSummaryCard("Total Target", "£0.00", VaadinIcon.BULLSEYE, "total-target"),
                createSummaryCard("Total Saved", "£0.00", VaadinIcon.PIGGY_BANK, "total-saved"),
                createSummaryCard("Goals", "0", VaadinIcon.LIST, "goals-count"),
                createSummaryCard("Completion", "0%", VaadinIcon.CHART, "completion-rate"),
                createSummaryCard("Monthly Goal", "£0.00", VaadinIcon.CALENDAR, "monthly-contribution")
        );

        return cardsLayout;
    }

    private VerticalLayout createSummaryCard(
            final String label,
            final String initialValue,
            final VaadinIcon icon,
            final String cardType
    ) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("summary-card");
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("flex", "1");

        Icon cardIcon = icon.create();
        cardIcon.setSize("24px");
        cardIcon.getStyle().set("color", "var(--lumo-primary-color)");

        Span valueSpan = new Span(initialValue);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(cardIcon, valueSpan, labelSpan);

        switch (cardType) {
            case "total-target":
                totalTargetAmountSpan = valueSpan;
                break;
            case "total-saved":
                totalSavedAmountSpan = valueSpan;
                break;
            case "goals-count":
                goalsCountSpan = valueSpan;
                break;
            case "completion-rate":
                completionRateSpan = valueSpan;
                break;
            case "monthly-contribution":
                recommendedContributionSpan = valueSpan;
                break;
        }

        return card;
    }

    private HorizontalLayout createToolbar() {
        Button addGoalButton = new Button("Add Goal", new Icon(VaadinIcon.PLUS));
        addGoalButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addGoalButton.addClickListener(event -> openGoalForm(null));

        filterComboBox = new ComboBox<>("Filter");
        filterComboBox.setItems("All Goals", "Active", "Completed", "High Priority", "Medium Priority", "Low Priority");
        filterComboBox.setValue("All Goals");
        filterComboBox.addValueChangeListener(event -> refreshData());

        HorizontalLayout toolbar = new HorizontalLayout(addGoalButton, filterComboBox);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("mobile-toolbar");

        return toolbar;
    }

    private Grid<SavingsGoalProgressDTO> createGrid() {
        grid = new Grid<>(SavingsGoalProgressDTO.class, false);
        grid.addClassName("savings-goals-grid");
        grid.setSizeFull();

        grid.addColumn(SavingsGoalProgressDTO::getGoalName)
                .setHeader("Goal Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(progress -> String.format("£%.2f", progress.getCurrentAmount()))
                .setHeader("Current Amount")
                .setAutoWidth(true);

        grid.addColumn(progress -> String.format("£%.2f", progress.getTargetAmount()))
                .setHeader("Target Amount")
                .setAutoWidth(true);

        grid.addComponentColumn(this::createProgressBar)
                .setHeader("Progress")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(progress -> {
            if (progress.getTargetDate() != null) {
                return progress.getTargetDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            }
            return "No target date";
        })
                .setHeader("Target Date")
                .setAutoWidth(true);

        grid.addColumn(progress -> {
            if (progress.getDaysRemaining() != null) {
                long days = progress.getDaysRemaining();
                if (days < 0) {
                    return "Overdue";
                } else if (days == 0) {
                    return "Today";
                } else if (days <= 30) {
                    return days + " days";
                } else {
                    long months = days / 30;
                    return months + " month" + (months > 1 ? "s" : "");
                }
            }
            return "-";
        })
                .setHeader("Time Remaining")
                .setAutoWidth(true);

        grid.addColumn(progress -> {
            if (progress.getRecommendedMonthlyContribution() != null) {
                return String.format("£%.2f/mo", progress.getRecommendedMonthlyContribution());
            }
            return "-";
        })
                .setHeader("Recommended")
                .setAutoWidth(true);

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setAutoWidth(true);

        return grid;
    }

    private VerticalLayout createProgressBar(final SavingsGoalProgressDTO progress) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMin(0);
        progressBar.setMax(100);

        double percentage = progress.getPercentageComplete().doubleValue();
        progressBar.setValue(Math.min(percentage, 100));

        if (percentage >= 100) {
            progressBar.getStyle().set("--lumo-primary-color", "var(--lumo-success-color)");
        } else if (progress.getStatus().equals("BEHIND")) {
            progressBar.getStyle().set("--lumo-primary-color", "var(--lumo-error-color)");
        }

        Span percentageLabel = new Span(String.format("%.1f%%", percentage));
        percentageLabel.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        layout.add(progressBar, percentageLabel);

        return layout;
    }

    private Span createStatusBadge(final SavingsGoalProgressDTO progress) {
        Span badge = new Span(getStatusLabel(progress.getStatus()));
        badge.getElement().getThemeList().add("badge");

        switch (progress.getStatus()) {
            case "COMPLETED":
                badge.getElement().getThemeList().add("success");
                break;
            case "ON_TRACK":
                badge.getElement().getThemeList().add("success");
                break;
            case "BEHIND":
                badge.getElement().getThemeList().add("error");
                break;
            case "OVERDUE":
                badge.getElement().getThemeList().add("error");
                break;
            default:
                badge.getElement().getThemeList().add("contrast");
        }

        return badge;
    }

    private String getStatusLabel(final String status) {
        return switch (status) {
            case "COMPLETED" -> "Completed";
            case "ON_TRACK" -> "On Track";
            case "BEHIND" -> "Behind";
            case "OVERDUE" -> "Overdue";
            default -> "Unknown";
        };
    }

    private HorizontalLayout createActionButtons(final SavingsGoalProgressDTO progress) {
        Button contributeButton = new Button(new Icon(VaadinIcon.PLUS_CIRCLE));
        contributeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
        contributeButton.addClassName("mobile-touch-target");
        contributeButton.getElement().setAttribute("title", "Record Contribution");
        contributeButton.getElement().setAttribute("aria-label", "Record contribution");
        contributeButton.addClickListener(event -> openContributionDialog(progress));

        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClassName("mobile-touch-target");
        editButton.getElement().setAttribute("title", "Edit Goal");
        editButton.getElement().setAttribute("aria-label", "Edit goal");
        editButton.addClickListener(event -> savingsGoalService.findByIdAsDto(progress.getGoalId())
                .ifPresent(this::openGoalForm));

        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.addClassName("mobile-touch-target");
        deleteButton.getElement().setAttribute("title", "Delete Goal");
        deleteButton.getElement().setAttribute("aria-label", "Delete goal");
        deleteButton.addClickListener(event -> deleteGoal(progress.getGoalId(), progress.getGoalName()));

        HorizontalLayout actions = new HorizontalLayout(contributeButton, editButton, deleteButton);
        actions.setSpacing(true);
        actions.addClassName("action-buttons");

        return actions;
    }

    private void setupDialogs() {
        savingsGoalForm = new SavingsGoalForm(savingsGoalService, authenticationContext, userRepository);
        savingsGoalForm.setSaveListener(goal -> {
            formDialog.close();
            refreshData();
        });
        savingsGoalForm.setCancelListener(() -> formDialog.close());

        formDialog = new Dialog();
        formDialog.setCloseOnEsc(true);
        formDialog.setCloseOnOutsideClick(false);
        formDialog.setModal(true);
        formDialog.setWidth("min(600px, 95vw)");
        formDialog.setMaxHeight("90vh");

        H3 formTitle = new H3("Savings Goal");
        formTitle.getStyle().set("margin", "0");

        VerticalLayout dialogLayout = new VerticalLayout(formTitle, savingsGoalForm);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        formDialog.add(dialogLayout);

        contributionDialog = new SavingsGoalContributionDialog(contributionService, savingsGoalService);
        contributionDialog.setSaveListener(contribution -> refreshData());
    }

    private void openGoalForm(final SavingsGoalDTO goal) {
        if (goal != null) {
            savingsGoalForm.setGoal(goal);
        } else {
            savingsGoalForm.clearForm();
        }
        formDialog.open();
    }

    private void openContributionDialog(final SavingsGoalProgressDTO progress) {
        contributionDialog.open(progress.getGoalId(), progress.getGoalName());
    }

    private void deleteGoal(final Long goalId, final String goalName) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setCloseOnEsc(true);
        confirmDialog.setCloseOnOutsideClick(false);

        H3 title = new H3("Delete Goal");
        Span message = new Span("Are you sure you want to delete \"" + goalName + "\"? This action cannot be undone.");

        Button confirmButton = new Button("Delete", event -> {
            try {
                savingsGoalService.deleteById(goalId);
                showSuccessNotification("Goal deleted successfully");
                refreshData();
                confirmDialog.close();
            } catch (Exception e) {
                showErrorNotification("Error deleting goal: " + e.getMessage());
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Cancel", event -> confirmDialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(title, message, buttonLayout);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        confirmDialog.add(dialogLayout);
        confirmDialog.open();
    }

    private void refreshData() {
        User currentUser = getCurrentUser();
        List<SavingsGoalProgressDTO> progressList = getFilteredGoals(currentUser);

        grid.setItems(progressList);
        updateSummaryCards(currentUser, progressList);
    }

    private List<SavingsGoalProgressDTO> getFilteredGoals(final User user) {
        String filter = filterComboBox.getValue();

        List<SavingsGoalProgressDTO> allProgress = savingsGoalService.calculateProgressForAllGoals(user);

        if (filter == null || filter.equals("All Goals")) {
            return allProgress;
        } else if (filter.equals("Active")) {
            return allProgress.stream()
                    .filter(p -> !p.getStatus().equals("COMPLETED"))
                    .toList();
        } else if (filter.equals("Completed")) {
            return allProgress.stream()
                    .filter(p -> p.getStatus().equals("COMPLETED"))
                    .toList();
        } else if (filter.equals("High Priority")) {
            return getProgressForPriority(user, Priority.HIGH);
        } else if (filter.equals("Medium Priority")) {
            return getProgressForPriority(user, Priority.MEDIUM);
        } else if (filter.equals("Low Priority")) {
            return getProgressForPriority(user, Priority.LOW);
        }

        return allProgress;
    }

    private List<SavingsGoalProgressDTO> getProgressForPriority(final User user, final Priority priority) {
        List<SavingsGoalDTO> goals = savingsGoalService.findByUserAndPriorityAsDto(user, priority);
        return goals.stream()
                .map(goal -> savingsGoalService.calculateProgress(goal.getGoalId()))
                .toList();
    }

    private void updateSummaryCards(final User user, final List<SavingsGoalProgressDTO> progressList) {
        BigDecimal totalTarget = savingsGoalService.getTotalTargetAmountByUser(user);
        BigDecimal totalSaved = savingsGoalService.getTotalCurrentAmountByUser(user);
        long goalsCount = progressList.size();

        totalTargetAmountSpan.setText(String.format("£%.2f", totalTarget != null ? totalTarget : BigDecimal.ZERO));
        totalSavedAmountSpan.setText(String.format("£%.2f", totalSaved != null ? totalSaved : BigDecimal.ZERO));
        goalsCountSpan.setText(String.valueOf(goalsCount));

        if (totalTarget != null && totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal completionRate = totalSaved.divide(totalTarget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            completionRateSpan.setText(String.format("%.1f%%", completionRate));
        } else {
            completionRateSpan.setText("0%");
        }

        BigDecimal totalRecommended = savingsGoalService.calculateTotalRecommendedMonthlyContribution(user);
        recommendedContributionSpan.setText(String.format("£%.2f", totalRecommended));
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
