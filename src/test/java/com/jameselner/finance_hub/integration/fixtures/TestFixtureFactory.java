package com.jameselner.finance_hub.integration.fixtures;

import com.jameselner.finance_hub.domain.*;
import com.jameselner.finance_hub.domain.enums.*;
import com.jameselner.finance_hub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.jameselner.finance_hub.integration.fixtures.TestDataBuilder.*;

/**
 * Factory for creating complete test data fixtures.
 * Provides convenient methods for setting up test scenarios.
 */
@Component
public class TestFixtureFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Creates a basic user with one account and two categories (expense + income).
     */
    public TestUserContext createBasicUser(String email) {
        User user = aUser()
                .withEmail(email)
                .withName("Test", "User")
                .withPasswordHash(passwordEncoder.encode("Test@123456"))
                .build();
        user = userRepository.save(user);

        Account account = anAccount()
                .forUser(user)
                .withName("Main Account")
                .withType(AccountType.CURRENT)
                .withBalance(new BigDecimal("1000.00"))
                .build();
        account = accountRepository.save(account);

        Category expenseCategory = aCategory()
                .forUser(user)
                .withName("Groceries")
                .asExpense()
                .build();
        expenseCategory = categoryRepository.save(expenseCategory);

        Category incomeCategory = aCategory()
                .forUser(user)
                .withName("Salary")
                .asIncome()
                .build();
        incomeCategory = categoryRepository.save(incomeCategory);

        return new TestUserContext(user, account, expenseCategory, incomeCategory);
    }

    /**
     * Creates a user with a complete financial profile including:
     * - Multiple accounts
     * - Multiple categories
     * - A debt
     * - A savings goal
     */
    public TestUserContext createFullFinancialProfile(String email) {
        TestUserContext ctx = createBasicUser(email);

        // Create additional accounts
        Account savingsAccount = anAccount()
                .forUser(ctx.user())
                .withName("Savings Account")
                .withType(AccountType.SAVINGS)
                .withBalance(new BigDecimal("5000.00"))
                .build();
        accountRepository.save(savingsAccount);

        // Create additional categories
        createCategory(ctx.user(), "Rent", CategoryType.EXPENSE);
        createCategory(ctx.user(), "Utilities", CategoryType.EXPENSE);
        createCategory(ctx.user(), "Entertainment", CategoryType.EXPENSE);
        createCategory(ctx.user(), "Transport", CategoryType.EXPENSE);

        // Create a debt
        Debt creditCard = aDebt()
                .forUser(ctx.user())
                .withName("Visa Card")
                .withType(DebtType.CREDIT_CARD)
                .withBalance(new BigDecimal("5000.00"), new BigDecimal("3500.00"))
                .withInterestRate(new BigDecimal("19.99"))
                .build();
        debtRepository.save(creditCard);

        // Create a savings goal
        SavingsGoal goal = aSavingsGoal()
                .forUser(ctx.user())
                .withName("Emergency Fund")
                .withTarget(new BigDecimal("10000.00"))
                .withCurrentAmount(new BigDecimal("2000.00"))
                .withPriority(Priority.HIGH)
                .build();
        savingsGoalRepository.save(goal);

        return ctx;
    }

    /**
     * Creates transactions for a user within a specific month.
     */
    public void createTransactionsForMonth(TestUserContext ctx, int year, int month, int count) {
        LocalDate startDate = LocalDate.of(year, month, 1);

        for (int i = 0; i < count; i++) {
            Transaction t = aTransaction()
                    .forAccount(ctx.account())
                    .inCategory(ctx.expenseCategory())
                    .asExpense(new BigDecimal("50.00").add(new BigDecimal(i * 10)))
                    .onDate(startDate.plusDays(i % 28))
                    .withDescription("Transaction " + (i + 1))
                    .build();
            transactionRepository.save(t);
        }
    }

    /**
     * Creates a budget for a user's expense category.
     */
    public Budget createBudgetForMonth(TestUserContext ctx, int year, int month, BigDecimal amount) {
        Budget budget = aBudget()
                .forUser(ctx.user())
                .forCategory(ctx.expenseCategory())
                .withAmount(amount)
                .forMonth(year, month)
                .build();
        return budgetRepository.save(budget);
    }

    /**
     * Creates a debt for a user.
     */
    public Debt createDebt(User user, String name, DebtType type, BigDecimal balance, BigDecimal interestRate) {
        Debt debt = aDebt()
                .forUser(user)
                .withName(name)
                .withType(type)
                .withBalance(balance, balance)
                .withInterestRate(interestRate)
                .build();
        return debtRepository.save(debt);
    }

    /**
     * Creates a savings goal for a user.
     */
    public SavingsGoal createSavingsGoal(User user, String name, BigDecimal target, BigDecimal current, Priority priority) {
        SavingsGoal goal = aSavingsGoal()
                .forUser(user)
                .withName(name)
                .withTarget(target)
                .withCurrentAmount(current)
                .withPriority(priority)
                .build();
        return savingsGoalRepository.save(goal);
    }

    private Category createCategory(User user, String name, CategoryType type) {
        Category cat = aCategory()
                .forUser(user)
                .withName(name)
                .build();
        if (type == CategoryType.INCOME) {
            cat.setCategoryType(CategoryType.INCOME);
        }
        return categoryRepository.save(cat);
    }

    /**
     * Context record for passing test user data between methods.
     */
    public record TestUserContext(
            User user,
            Account account,
            Category expenseCategory,
            Category incomeCategory
    ) {}
}
