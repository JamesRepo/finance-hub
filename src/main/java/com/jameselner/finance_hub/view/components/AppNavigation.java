package com.jameselner.finance_hub.view.components;

import com.jameselner.finance_hub.view.AccountsView;
import com.jameselner.finance_hub.view.BudgetTrackingView;
import com.jameselner.finance_hub.view.CategoriesView;
import com.jameselner.finance_hub.view.DashboardView;
import com.jameselner.finance_hub.view.DebtManagementView;
import com.jameselner.finance_hub.view.HolidaysView;
import com.jameselner.finance_hub.view.HousingExpensesView;
import com.jameselner.finance_hub.view.IncomeTrackingView;
import com.jameselner.finance_hub.view.MonthlySummaryView;
import com.jameselner.finance_hub.view.SavingsGoalsView;
import com.jameselner.finance_hub.view.SubscriptionsView;
import com.jameselner.finance_hub.view.TransactionView;
import com.jameselner.finance_hub.view.YearlySummaryView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class AppNavigation extends VerticalLayout {

    public AppNavigation() {
        setSpacing(false);
        setPadding(false);
        setWidth("280px");
        addClassName("app-navigation");
        getStyle()
                .set("background-color", "white")
                .set("border-right", "1px solid #e2e8f0");

        add(
                createSection("OVERVIEW", createMainNav()),
                createSection("MANAGEMENT", createFinanceNav()),
                createSection("ADMIN", createAdminNav())
        );
    }

    private Component createSection(final String title, final SideNav nav) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.getStyle().set("margin-bottom", "1.5rem");

        Span sectionTitle = new Span(title);
        sectionTitle.getStyle()
                .set("font-size", "0.7rem")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("color", "#9ca3af")
                .set("margin-bottom", "0.5rem")
                .set("letter-spacing", "0.08em")
                .set("padding-left", "1rem");

        section.add(sectionTitle, nav);
        return section;
    }

    private SideNav createMainNav() {
        SideNav nav = new SideNav();

        SideNavItem dashboard = new SideNavItem("\uD83D\uDCCA Dashboard", DashboardView.class);
        nav.addItem(dashboard);

        SideNavItem monthlySummary = new SideNavItem("\uD83D\uDCC5 Monthly Summary", MonthlySummaryView.class);
        nav.addItem(monthlySummary);

        SideNavItem yearlySummary = new SideNavItem("\uD83D\uDCC6 Yearly Summary", YearlySummaryView.class);
        nav.addItem(yearlySummary);

        return nav;
    }

    private SideNav createFinanceNav() {
        SideNav nav = new SideNav();

        SideNavItem accounts = new SideNavItem("\uD83E\uDEAA Accounts", AccountsView.class);
        nav.addItem(accounts);

        SideNavItem transactions = new SideNavItem("\uD83D\uDCB3 Transactions", TransactionView.class);
        nav.addItem(transactions);

        SideNavItem income = new SideNavItem("\uD83D\uDCB5 Income", IncomeTrackingView.class);
        nav.addItem(income);

        SideNavItem budget = new SideNavItem("\uD83D\uDCB8 Budget", BudgetTrackingView.class);
        nav.addItem(budget);

        SideNavItem housing = new SideNavItem("\uD83C\uDFE0 Housing", HousingExpensesView.class);
        nav.addItem(housing);

        SideNavItem holidays = new SideNavItem("✈\uFE0F Holidays", HolidaysView.class);
        nav.addItem(holidays);

        SideNavItem savingsGoals = new SideNavItem("\uD83D\uDC8E Savings Goals", SavingsGoalsView.class);
        nav.addItem(savingsGoals);

        SideNavItem debts = new SideNavItem("\uD83C\uDFE6 Debts", DebtManagementView.class);
        nav.addItem(debts);

        SideNavItem subscriptions = new SideNavItem("\uD83D\uDD16 Subscriptions", SubscriptionsView.class);
        nav.addItem(subscriptions);

        return nav;
    }

    private SideNav createAdminNav() {
        SideNav nav = new SideNav();

        SideNavItem categories = new SideNavItem("\uD83D\uDDC2 Categories", CategoriesView.class);
        nav.addItem(categories);

        return nav;
    }
}