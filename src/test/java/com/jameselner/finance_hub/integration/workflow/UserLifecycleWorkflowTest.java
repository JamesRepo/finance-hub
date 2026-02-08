package com.jameselner.finance_hub.integration.workflow;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.*;
import com.jameselner.finance_hub.dto.*;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Lifecycle Workflow Integration Tests")
class UserLifecycleWorkflowTest extends TransactionalIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private SavingsGoalService savingsGoalService;

    @Test
    @DisplayName("Should complete full user financial journey from registration to reporting")
    void shouldCompleteFullUserFinancialJourney() {
        // Step 1: User Registration
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("journey@test.com");
        registrationDto.setPassword("SecurePass@123");
        registrationDto.setFirstName("Journey");
        registrationDto.setLastName("User");
        User user = userService.registerUser(registrationDto);
        assertNotNull(user.getUserId(), "User should be created with ID");

        // Step 2: Create Account
        AccountDTO accountDto = AccountDTO.builder()
                .userId(user.getUserId())
                .accountName("Main Checking")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("5000.00"))
                .currency("GBP")
                .build();
        AccountDTO account = accountService.createFromDto(accountDto);
        assertNotNull(account.getAccountId(), "Account should be created");

        // Step 3: Create Categories
        CategoryDTO expenseCategory = CategoryDTO.builder()
                .userId(user.getUserId())
                .categoryName("Food & Dining")
                .categoryType(CategoryType.EXPENSE)
                .colorCode("#FF5733")
                .build();
        expenseCategory = categoryService.createFromDto(expenseCategory);

        CategoryDTO incomeCategory = CategoryDTO.builder()
                .userId(user.getUserId())
                .categoryName("Salary")
                .categoryType(CategoryType.INCOME)
                .colorCode("#33FF57")
                .build();
        incomeCategory = categoryService.createFromDto(incomeCategory);

        // Step 4: Create Budget
        LocalDate budgetStart = LocalDate.now().withDayOfMonth(1);
        LocalDate budgetEnd = budgetStart.plusMonths(1).minusDays(1);
        BudgetDTO budgetDto = BudgetDTO.builder()
                .userId(user.getUserId())
                .categoryId(expenseCategory.getCategoryId())
                .amount(new BigDecimal("500.00"))
                .periodType(PeriodType.MONTHLY)
                .startDate(budgetStart)
                .endDate(budgetEnd)
                .build();
        BudgetDTO budget = budgetService.createBudgetFromDto(budgetDto);
        assertNotNull(budget.getBudgetId(), "Budget should be created");

        // Step 5: Record Income Transaction
        TransactionDTO income = TransactionDTO.builder()
                .accountId(account.getAccountId())
                .categoryId(incomeCategory.getCategoryId())
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("3000.00"))
                .transactionDate(LocalDate.now().minusDays(15))
                .description("Monthly salary")
                .build();
        transactionService.createTransactionFromDto(income);

        // Step 6: Record Expense Transactions
        for (int i = 1; i <= 5; i++) {
            TransactionDTO expense = TransactionDTO.builder()
                    .accountId(account.getAccountId())
                    .categoryId(expenseCategory.getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("50.00"))
                    .transactionDate(LocalDate.now().minusDays(i))
                    .description("Meal " + i)
                    .build();
            transactionService.createTransactionFromDto(expense);
        }

        // Step 7: Create Savings Goal
        SavingsGoalDTO goalDto = SavingsGoalDTO.builder()
                .userId(user.getUserId())
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusYears(1))
                .priority(Priority.HIGH)
                .build();
        SavingsGoalDTO goal = savingsGoalService.createSavingsGoalFromDto(goalDto);

        // Step 8: Add Contribution to Savings Goal
        savingsGoalService.addContribution(goal.getGoalId(), new BigDecimal("500.00"));

        // Verify: Check all data is properly linked
        List<TransactionDTO> transactions = transactionService.findAllAsDto();
        assertEquals(6, transactions.size(), "Should have 6 transactions (1 income + 5 expenses)");

        BigDecimal totalExpenses = transactionService.sumByUserAndTypeAndDateRange(
                user, TransactionType.EXPENSE, budgetStart, budgetEnd);
        assertEquals(new BigDecimal("250.00"), totalExpenses, "Total expenses should be 250");

        BigDecimal spentOnBudget = budgetService.calculateSpentAmount(
                user.getUserId(), expenseCategory.getCategoryId(),
                budgetStart, budgetEnd);
        assertEquals(new BigDecimal("250.00"), spentOnBudget, "Budget spending should match");

        SavingsGoalDTO updatedGoal = savingsGoalService.findByIdAsDto(goal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("500.00"), updatedGoal.getCurrentAmount(), "Savings goal should have 500");
    }

    @Test
    @DisplayName("Should handle profile updates after initial setup")
    void shouldHandleProfileUpdatesAfterInitialSetup() {
        // Register user
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("update@test.com");
        dto.setPassword("SecurePass@123");
        dto.setFirstName("Original");
        dto.setLastName("Name");
        User user = userService.registerUser(dto);

        // Create some financial data
        AccountDTO account = AccountDTO.builder()
                .userId(user.getUserId())
                .accountName("My Account")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("1000.00"))
                .currency("GBP")
                .build();
        accountService.createFromDto(account);

        // Update profile
        User updated = userService.updateProfile(user, "Updated", "Person", "newemail@test.com");

        // Verify profile updated but data intact
        assertEquals("Updated", updated.getFirstName());
        assertEquals("newemail@test.com", updated.getEmail());

        List<AccountDTO> accounts = accountService.findByUserIdAsDto(user.getUserId());
        assertEquals(1, accounts.size(), "Account should still exist after profile update");
    }

    @Test
    @DisplayName("Should handle password change during active session")
    void shouldHandlePasswordChangeDuringActiveSession() {
        // Register user
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("password@test.com");
        dto.setPassword("OldSecure@123");
        dto.setFirstName("Password");
        dto.setLastName("Changer");
        User user = userService.registerUser(dto);

        // Create financial data
        SavingsGoalDTO goal = SavingsGoalDTO.builder()
                .userId(user.getUserId())
                .goalName("Test Goal")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .priority(Priority.MEDIUM)
                .build();
        savingsGoalService.createSavingsGoalFromDto(goal);

        // Change password
        userService.changePassword(user, "OldSecure@123", "NewSecure@456");

        // Verify data still accessible
        List<SavingsGoalDTO> goals = savingsGoalService.findByUserAsDto(user);
        assertEquals(1, goals.size(), "Savings goal should still be accessible after password change");
    }

    @Test
    @DisplayName("Should track multiple financial goals simultaneously")
    void shouldTrackMultipleFinancialGoalsSimultaneously() {
        // Setup user
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("multigoal@test.com");
        dto.setPassword("SecurePass@123");
        dto.setFirstName("Multi");
        dto.setLastName("Goal");
        User user = userService.registerUser(dto);

        // Create multiple savings goals
        String[] goalNames = {"Emergency Fund", "Vacation", "New Car", "Home Deposit"};
        Priority[] priorities = {Priority.HIGH, Priority.LOW, Priority.MEDIUM, Priority.HIGH};
        BigDecimal[] targets = {
                new BigDecimal("10000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("50000.00")
        };

        for (int i = 0; i < goalNames.length; i++) {
            SavingsGoalDTO goal = SavingsGoalDTO.builder()
                    .userId(user.getUserId())
                    .goalName(goalNames[i])
                    .targetAmount(targets[i])
                    .currentAmount(BigDecimal.ZERO)
                    .targetDate(LocalDate.now().plusMonths(6 + i * 6))
                    .priority(priorities[i])
                    .build();
            savingsGoalService.createSavingsGoalFromDto(goal);
        }

        // Verify all goals
        List<SavingsGoalDTO> allGoals = savingsGoalService.findByUserAsDto(user);
        assertEquals(4, allGoals.size());

        // Verify high priority goals
        List<SavingsGoalDTO> highPriority = savingsGoalService.findByUserAndPriorityAsDto(user, Priority.HIGH);
        assertEquals(2, highPriority.size());

        // Verify total target
        BigDecimal totalTarget = savingsGoalService.getTotalTargetAmountByUser(user);
        assertEquals(new BigDecimal("78000.00"), totalTarget);
    }
}
