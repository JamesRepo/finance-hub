package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.view.DashboardView;
import com.jameselner.finance_hub.view.TransactionView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class AppNavigation extends VerticalLayout {

    public AppNavigation() {
        setSpacing(false);
        setPadding(false);
        setWidth("260px");
        getStyle()
                .set("background-color", "white")
                .set("border-right", "1px solid #e2e8f0");

        add(
                createSection("MAIN", createMainNav()),
                createSection("FINANCE", createFinanceNav()),
                createSection("TOOLS", createToolsNav())
        );
    }

    private Component createSection(final String title, final SideNav nav) {
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
                .set("letter-spacing", "0.05em")
                .set("padding-left", "1rem");

        section.add(sectionTitle, nav);
        return section;
    }

    private SideNav createMainNav() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("Dashboard", DashboardView.class);
        dashboard.setPrefixComponent(VaadinIcon.DASHBOARD.create());
        nav.addItem(dashboard);

        SideNavItem analytics = new SideNavItem("Analytics");
        analytics.setPrefixComponent(VaadinIcon.CHART.create());
        nav.addItem(analytics);

        SideNavItem reports = new SideNavItem("Reports");
        reports.setPrefixComponent(VaadinIcon.FILE_TEXT.create());
        nav.addItem(reports);

        return nav;
    }

    private SideNav createFinanceNav() {
        SideNav nav = new SideNav();

        SideNavItem accounts = new SideNavItem("Accounts");
        accounts.setPrefixComponent(VaadinIcon.WALLET.create());
        nav.addItem(accounts);

        SideNavItem transactions = new SideNavItem("Transactions", TransactionView.class);
        transactions.setPrefixComponent(VaadinIcon.EXCHANGE.create());
        nav.addItem(transactions);

        SideNavItem budget = new SideNavItem("Budget");
        budget.setPrefixComponent(VaadinIcon.PIGGY_BANK.create());
        nav.addItem(budget);

        SideNavItem investments = new SideNavItem("Investments");
        investments.setPrefixComponent(VaadinIcon.TRENDING_UP.create());
        nav.addItem(investments);

        SideNavItem bills = new SideNavItem("Bills");
        bills.setPrefixComponent(VaadinIcon.INVOICE.create());
        nav.addItem(bills);

        return nav;
    }

    private SideNav createToolsNav() {
        SideNav nav = new SideNav();

        SideNavItem goals = new SideNavItem("Goals");
        goals.setPrefixComponent(VaadinIcon.FLAG.create());
        nav.addItem(goals);

        SideNavItem calculator = new SideNavItem("Calculator");
        calculator.setPrefixComponent(VaadinIcon.CALC.create());
        nav.addItem(calculator);

        SideNavItem settings = new SideNavItem("Settings");
        settings.setPrefixComponent(VaadinIcon.COG.create());
        nav.addItem(settings);

        return nav;
    }
}