package com.jameselner.finance_hub.integration.fixtures;

import com.jameselner.finance_hub.domain.*;
import com.jameselner.finance_hub.domain.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builder classes for creating test entities with fluent API.
 */
public class TestDataBuilder {

    // ========== User Builder ==========
    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String email = "testuser@example.com";
        private String passwordHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"; // BCrypt "password"
        private String firstName = "Test";
        private String lastName = "User";

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder withName(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
            return this;
        }

        public User build() {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return user;
        }
    }

    // ========== Account Builder ==========
    public static AccountBuilder anAccount() {
        return new AccountBuilder();
    }

    public static class AccountBuilder {
        private User user;
        private String accountName = "Test Account";
        private AccountType accountType = AccountType.CURRENT;
        private BigDecimal balance = BigDecimal.ZERO;
        private String currency = "GBP";
        private Boolean isActive = true;

        public AccountBuilder forUser(User user) {
            this.user = user;
            return this;
        }

        public AccountBuilder withName(String name) {
            this.accountName = name;
            return this;
        }

        public AccountBuilder withType(AccountType type) {
            this.accountType = type;
            return this;
        }

        public AccountBuilder withBalance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public AccountBuilder withCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public AccountBuilder inactive() {
            this.isActive = false;
            return this;
        }

        public Account build() {
            Account account = new Account();
            account.setUser(user);
            account.setAccountName(accountName);
            account.setAccountType(accountType);
            account.setBalance(balance);
            account.setCurrency(currency);
            account.setIsActive(isActive);
            return account;
        }
    }

    // ========== Category Builder ==========
    public static CategoryBuilder aCategory() {
        return new CategoryBuilder();
    }

    public static class CategoryBuilder {
        private User user;
        private String categoryName = "Test Category";
        private CategoryType categoryType = CategoryType.EXPENSE;
        private String colorCode = "#FF5733";
        private Boolean isSystem = false;

        public CategoryBuilder forUser(User user) {
            this.user = user;
            return this;
        }

        public CategoryBuilder withName(String name) {
            this.categoryName = name;
            return this;
        }

        public CategoryBuilder asExpense() {
            this.categoryType = CategoryType.EXPENSE;
            return this;
        }

        public CategoryBuilder asIncome() {
            this.categoryType = CategoryType.INCOME;
            return this;
        }

        public CategoryBuilder withColor(String colorCode) {
            this.colorCode = colorCode;
            return this;
        }

        public CategoryBuilder asSystem() {
            this.isSystem = true;
            this.user = null;
            return this;
        }

        public Category build() {
            Category category = new Category();
            category.setUser(user);
            category.setCategoryName(categoryName);
            category.setCategoryType(categoryType);
            category.setColorCode(colorCode);
            category.setIsSystem(isSystem);
            return category;
        }
    }

    // ========== Transaction Builder ==========
    public static TransactionBuilder aTransaction() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private Account account;
        private Category category;
        private TransactionType transactionType = TransactionType.EXPENSE;
        private BigDecimal amount = new BigDecimal("100.00");
        private LocalDate transactionDate = LocalDate.now();
        private String placeVenue;
        private String description = "Test transaction";

        public TransactionBuilder forAccount(Account account) {
            this.account = account;
            return this;
        }

        public TransactionBuilder inCategory(Category category) {
            this.category = category;
            return this;
        }

        public TransactionBuilder asExpense(BigDecimal amount) {
            this.transactionType = TransactionType.EXPENSE;
            this.amount = amount;
            return this;
        }

        public TransactionBuilder asIncome(BigDecimal amount) {
            this.transactionType = TransactionType.INCOME;
            this.amount = amount;
            return this;
        }

        public TransactionBuilder onDate(LocalDate date) {
            this.transactionDate = date;
            return this;
        }

        public TransactionBuilder at(String placeVenue) {
            this.placeVenue = placeVenue;
            return this;
        }

        public TransactionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Transaction build() {
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setCategory(category);
            transaction.setTransactionType(transactionType);
            transaction.setAmount(amount);
            transaction.setTransactionDate(transactionDate);
            transaction.setPlaceVenue(placeVenue);
            transaction.setDescription(description);
            return transaction;
        }
    }

    // ========== Budget Builder ==========
    public static BudgetBuilder aBudget() {
        return new BudgetBuilder();
    }

    public static class BudgetBuilder {
        private User user;
        private Category category;
        private BigDecimal amount = new BigDecimal("500.00");
        private PeriodType periodType = PeriodType.MONTHLY;
        private LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        private LocalDate endDate = LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1);

        public BudgetBuilder forUser(User user) {
            this.user = user;
            return this;
        }

        public BudgetBuilder forCategory(Category category) {
            this.category = category;
            return this;
        }

        public BudgetBuilder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BudgetBuilder withPeriodType(PeriodType periodType) {
            this.periodType = periodType;
            return this;
        }

        public BudgetBuilder forPeriod(LocalDate start, LocalDate end) {
            this.startDate = start;
            this.endDate = end;
            return this;
        }

        public BudgetBuilder forMonth(int year, int month) {
            this.startDate = LocalDate.of(year, month, 1);
            this.endDate = this.startDate.plusMonths(1).minusDays(1);
            return this;
        }

        public Budget build() {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setCategory(category);
            budget.setAmount(amount);
            budget.setPeriodType(periodType);
            budget.setStartDate(startDate);
            budget.setEndDate(endDate);
            return budget;
        }
    }

    // ========== Debt Builder ==========
    public static DebtBuilder aDebt() {
        return new DebtBuilder();
    }

    public static class DebtBuilder {
        private User user;
        private String debtName = "Test Debt";
        private DebtType debtType = DebtType.CREDIT_CARD;
        private BigDecimal principalAmount = new BigDecimal("10000.00");
        private BigDecimal currentBalance = new BigDecimal("8000.00");
        private BigDecimal interestRate = new BigDecimal("18.99");
        private LocalDate startDate = LocalDate.now().minusMonths(6);
        private LocalDate targetPayoffDate;
        private BigDecimal minimumPayment = new BigDecimal("200.00");

        public DebtBuilder forUser(User user) {
            this.user = user;
            return this;
        }

        public DebtBuilder withName(String name) {
            this.debtName = name;
            return this;
        }

        public DebtBuilder withType(DebtType type) {
            this.debtType = type;
            return this;
        }

        public DebtBuilder withPrincipal(BigDecimal principal) {
            this.principalAmount = principal;
            return this;
        }

        public DebtBuilder withCurrentBalance(BigDecimal balance) {
            this.currentBalance = balance;
            return this;
        }

        public DebtBuilder withBalance(BigDecimal principal, BigDecimal current) {
            this.principalAmount = principal;
            this.currentBalance = current;
            return this;
        }

        public DebtBuilder withInterestRate(BigDecimal rate) {
            this.interestRate = rate;
            return this;
        }

        public DebtBuilder startedOn(LocalDate date) {
            this.startDate = date;
            return this;
        }

        public DebtBuilder withTargetPayoffDate(LocalDate date) {
            this.targetPayoffDate = date;
            return this;
        }

        public DebtBuilder withMinimumPayment(BigDecimal payment) {
            this.minimumPayment = payment;
            return this;
        }

        public Debt build() {
            Debt debt = new Debt();
            debt.setUser(user);
            debt.setDebtName(debtName);
            debt.setDebtType(debtType);
            debt.setPrincipalAmount(principalAmount);
            debt.setCurrentBalance(currentBalance);
            debt.setInterestRate(interestRate);
            debt.setStartDate(startDate);
            debt.setTargetPayoffDate(targetPayoffDate);
            debt.setMinimumPayment(minimumPayment);
            return debt;
        }
    }

    // ========== SavingsGoal Builder ==========
    public static SavingsGoalBuilder aSavingsGoal() {
        return new SavingsGoalBuilder();
    }

    public static class SavingsGoalBuilder {
        private User user;
        private String goalName = "Test Goal";
        private BigDecimal targetAmount = new BigDecimal("5000.00");
        private BigDecimal currentAmount = BigDecimal.ZERO;
        private LocalDate targetDate = LocalDate.now().plusMonths(12);
        private Priority priority = Priority.MEDIUM;

        public SavingsGoalBuilder forUser(User user) {
            this.user = user;
            return this;
        }

        public SavingsGoalBuilder withName(String name) {
            this.goalName = name;
            return this;
        }

        public SavingsGoalBuilder withTarget(BigDecimal amount) {
            this.targetAmount = amount;
            return this;
        }

        public SavingsGoalBuilder withCurrentAmount(BigDecimal amount) {
            this.currentAmount = amount;
            return this;
        }

        public SavingsGoalBuilder withTargetDate(LocalDate date) {
            this.targetDate = date;
            return this;
        }

        public SavingsGoalBuilder withPriority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public SavingsGoal build() {
            SavingsGoal goal = new SavingsGoal();
            goal.setUser(user);
            goal.setGoalName(goalName);
            goal.setTargetAmount(targetAmount);
            goal.setCurrentAmount(currentAmount);
            goal.setTargetDate(targetDate);
            goal.setPriority(priority);
            return goal;
        }
    }
}
