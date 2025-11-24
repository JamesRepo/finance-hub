package com.jameselner.finance_hub.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.spring.security.AuthenticationContext;

@CssImport("./styles/finance-dashboard.css")
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public MainLayout(final AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        createHeader();
        createDrawer();
        createMainContent();

        addClassName("finance-dashboard");
    }

    /**
     * Creates the top navigation bar with logo, navigation links, and user section.
     * Uses Vaadin's AppLayout navbar area for sticky positioning.
     */
    private void createHeader() {
        HorizontalLayout logo = new HorizontalLayout();
        logo.setAlignItems(FlexComponent.Alignment.CENTER);
        logo.setSpacing(true);

        Icon dollarIcon = VaadinIcon.DOLLAR.create();
        dollarIcon.setColor("#2563eb");
        dollarIcon.setSize("24px");

        Span logoText = new Span("Finance Hub");
        logoText.addClassName("logo-text");
        logoText.getStyle()
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("color", "#2563eb");

        logo.add(dollarIcon, logoText);

        // Navigation tabs (Dashboard, Reports, Budget, Settings)
        Tab dashboardTab = createNavTab("Dashboard", VaadinIcon.DASHBOARD, true);
        Tab transactionsTab = createNavTab("Transactions", VaadinIcon.CREDIT_CARD, false);
        Tab reportsTab = createNavTab("Reports", VaadinIcon.CHART_LINE, false);
        Tab budgetTab = createNavTab("Budget", VaadinIcon.WALLET, false);

        Tabs navTabs = new Tabs(dashboardTab, transactionsTab, reportsTab, budgetTab);
        navTabs.addClassName("nav-tabs");
        navTabs.getStyle()
                .set("margin-left", "auto")
                .set("margin-right", "auto");

        Button notificationBtn = new Button(VaadinIcon.BELL.create());
        notificationBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        notificationBtn.getStyle().set("color", "#64748b");

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(), click -> {
            authenticationContext.logout();
        });

        HorizontalLayout userSection = new HorizontalLayout(notificationBtn, logoutButton);
        userSection.setSpacing(true);
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(logo, navTabs, userSection);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
                .set("background-color", "white")
                .set("border-bottom", "1px solid #e2e8f0")
                .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.1)");

        addToNavbar(header);
    }

    /**
     * Create navigation tabs with icons and text.
     */
    private Tab createNavTab(final String label, final VaadinIcon iconType, final boolean selected) {
        Icon icon = iconType.create();
        icon.setSize("16px");

        Span text = new Span(label);
        text.getStyle().set("font-weight", "500");

        HorizontalLayout tabContent = new HorizontalLayout(icon, text);
        tabContent.setSpacing(true);
        tabContent.setAlignItems(FlexComponent.Alignment.CENTER);

        Tab tab = new Tab(tabContent);
        if (selected) {
            tab.getStyle()
                    .set("color", "#2563eb")
                    .set("background-color", "#f8fafc");
        } else {
            tab.getStyle().set("color", "#64748b");
        }

        return tab;
    }

    /**
     * Creates the sidebar/drawer with navigation menu items.
     */
    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSpacing(false);
        drawer.setPadding(true);
        drawer.setWidth("260px");
        drawer.getStyle()
                .set("background-color", "white")
                .set("border-right", "1px solid #e2e8f0");

        // Main section
        drawer.add(createSidebarSection("MAIN",
                createMenuItem("Dashboard", VaadinIcon.DASHBOARD, true),
                createMenuItem("Analytics", VaadinIcon.CHART, false),
                createMenuItem("Reports", VaadinIcon.FILE_TEXT, false)
        ));

        // Finance section
        drawer.add(createSidebarSection("FINANCE",
                createMenuItem("Accounts", VaadinIcon.WALLET, false),
                createMenuItem("Transactions", VaadinIcon.EXCHANGE, false),
                createMenuItem("Budget", VaadinIcon.PIGGY_BANK, false),
                createMenuItem("Investments", VaadinIcon.TRENDING_UP, false),
                createMenuItem("Bills", VaadinIcon.INVOICE, false)
        ));

        // Tools section
        drawer.add(createSidebarSection("TOOLS",
                createMenuItem("Goals", VaadinIcon.FLAG, false),
                createMenuItem("Calculator", VaadinIcon.CALC, false),
                createMenuItem("Settings", VaadinIcon.COG, false)
        ));

        addToDrawer(drawer);
    }

    /**
     * Creates a sidebar section with a title and menu items.
     */
    private Component createSidebarSection(final String title, final Component... items) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.getStyle().set("margin-bottom", "2rem");

        Span sectionTitle = new Span(title);
        sectionTitle.getStyle()
                .set("font-size", "0.75rem")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("color", "#64748b")
                .set("margin-bottom", "0.75rem")
                .set("letter-spacing", "0.05em");

        section.add(sectionTitle);
        section.add(items);

        return section;
    }

    /**
     * Creates a sidebar menu item with icon and label.
     * Active state is indicated by different styling.
     */
    private Button createMenuItem(final String label, final VaadinIcon iconType, final boolean active) {
        Icon icon = iconType.create();
        icon.setSize("28px");

        Button menuItem = new Button(label, icon);
        menuItem.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        menuItem.setWidthFull();
        menuItem.getStyle()
                .set("justify-content", "flex-start")
                .set("padding", "1rem")
                .set("border-radius", "0.5rem");

        if (active) {
            menuItem.getStyle()
                    .set("background-color", "#2563eb")
                    .set("color", "white");
        } else {
            menuItem.getStyle().set("color", "#64748b");
        }

        return menuItem;
    }

    /**
     * Creates the main content area where dashboard components will be displayed.
     * This includes all the cards, charts, and forms.
     */
    private void createMainContent() {
        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSpacing(true);
        contentArea.setPadding(true);
        contentArea.setSizeFull();
        contentArea.getStyle()
                .set("background-color", "#f8fafc")
                .set("overflow-y", "auto");

        // Page header
        H1 pageTitle = new H1("Dashboard");
        pageTitle.getStyle()
                .set("font-size", "2rem")
                .set("margin-bottom", "0.5rem");

        Span pageSubtitle = new Span("Welcome back! Here's your financial overview.");
        pageSubtitle.getStyle()
                .set("color", "#64748b")
                .set("margin-bottom", "2rem");

        VerticalLayout pageHeader = new VerticalLayout(pageTitle, pageSubtitle);
        pageHeader.setSpacing(false);
        pageHeader.setPadding(false);

        contentArea.add(pageHeader);

        // Add dashboard components
//        contentArea.add(new SummaryCardsComponent());
//        contentArea.add(new ChartAndTransactionsComponent());
//        contentArea.add(new DebtAndSavingsComponent());
//        contentArea.add(new YearlySummaryComponent());
//        contentArea.add(new TransactionFormComponent());

        setContent(contentArea);
    }
}
