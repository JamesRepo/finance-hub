package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory.TestUserContext;
import com.jameselner.finance_hub.service.BudgetService;
import com.jameselner.finance_hub.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BudgetService Integration Tests")
class BudgetServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestFixtureFactory fixtureFactory;

    private TestUserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = fixtureFactory.createBasicUser("budget-test@example.com");
    }

    @Nested
    @DisplayName("Budget CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should create monthly budget")
        void shouldCreateMonthlyBudget() {
            BudgetDTO dto = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 31))
                    .build();

            BudgetDTO created = budgetService.createBudgetFromDto(dto);

            assertNotNull(created.getBudgetId());
            assertEquals(new BigDecimal("500.00"), created.getAmount());
            assertEquals(PeriodType.MONTHLY, created.getPeriodType());
        }

        @Test
        @DisplayName("Should update budget")
        void shouldUpdateBudget() {
            BudgetDTO dto = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 2, 28))
                    .build();
            BudgetDTO created = budgetService.createBudgetFromDto(dto);

            created.setAmount(new BigDecimal("600.00"));
            BudgetDTO updated = budgetService.updateBudgetFromDto(created);

            assertEquals(new BigDecimal("600.00"), updated.getAmount());
        }

        @Test
        @DisplayName("Should delete budget")
        void shouldDeleteBudget() {
            BudgetDTO dto = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 3, 1))
                    .endDate(LocalDate.of(2025, 3, 31))
                    .build();
            BudgetDTO created = budgetService.createBudgetFromDto(dto);

            budgetService.deleteById(created.getBudgetId());

            Optional<BudgetDTO> found = budgetService.findByIdAsDto(created.getBudgetId());
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should reject overlapping budgets for same category")
        void shouldRejectOverlappingBudgets() {
            BudgetDTO budget1 = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 4, 1))
                    .endDate(LocalDate.of(2025, 4, 30))
                    .build();
            budgetService.createBudgetFromDto(budget1);

            BudgetDTO overlapping = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("600.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 4, 15))
                    .endDate(LocalDate.of(2025, 5, 14))
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> budgetService.createBudgetFromDto(overlapping));
        }

        @Test
        @DisplayName("Should allow non-overlapping budgets for same category")
        void shouldAllowNonOverlappingBudgets() {
            BudgetDTO budget1 = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 5, 1))
                    .endDate(LocalDate.of(2025, 5, 31))
                    .build();
            budgetService.createBudgetFromDto(budget1);

            BudgetDTO budget2 = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("600.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2025, 6, 1))
                    .endDate(LocalDate.of(2025, 6, 30))
                    .build();

            BudgetDTO created = budgetService.createBudgetFromDto(budget2);
            assertNotNull(created.getBudgetId());
        }
    }

    @Nested
    @DisplayName("Budget Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find budgets by user")
        void shouldFindBudgetsByUser() {
            for (int i = 0; i < 3; i++) {
                BudgetDTO dto = BudgetDTO.builder()
                        .userId(userContext.user().getUserId())
                        .categoryId(userContext.expenseCategory().getCategoryId())
                        .amount(new BigDecimal("500.00"))
                        .periodType(PeriodType.MONTHLY)
                        .startDate(LocalDate.of(2025, 7 + i, 1))
                        .endDate(LocalDate.of(2025, 7 + i, 1).plusMonths(1).minusDays(1))
                        .build();
                budgetService.createBudgetFromDto(dto);
            }

            List<BudgetDTO> budgets = budgetService.findByUserIdAsDto(userContext.user().getUserId());

            assertEquals(3, budgets.size());
        }
    }

    @Nested
    @DisplayName("Spending Calculations")
    class SpendingCalculationTests {

        @Test
        @DisplayName("Should calculate spent amount correctly")
        void shouldCalculateSpentAmountCorrectly() {
            LocalDate startDate = LocalDate.of(2025, 10, 1);
            LocalDate endDate = LocalDate.of(2025, 10, 31);

            // Create budget
            BudgetDTO budget = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            budgetService.createBudgetFromDto(budget);

            // Create transactions in budget period
            for (int i = 0; i < 3; i++) {
                TransactionDTO txn = TransactionDTO.builder()
                        .accountId(userContext.account().getAccountId())
                        .categoryId(userContext.expenseCategory().getCategoryId())
                        .transactionType(TransactionType.EXPENSE)
                        .amount(new BigDecimal("100.00"))
                        .transactionDate(LocalDate.of(2025, 10, 10 + i))
                        .description("Budget transaction " + i)
                        .build();
                transactionService.createTransactionFromDto(txn);
            }

            // Calculate spent
            BigDecimal spent = budgetService.calculateSpentAmount(
                    userContext.user(),
                    userContext.expenseCategory(),
                    startDate,
                    endDate
            );

            assertEquals(new BigDecimal("300.00"), spent);
        }

        @Test
        @DisplayName("Should exclude transactions outside date range")
        void shouldExcludeTransactionsOutsideDateRange() {
            LocalDate startDate = LocalDate.of(2025, 11, 1);
            LocalDate endDate = LocalDate.of(2025, 11, 30);

            // Create budget
            BudgetDTO budget = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            budgetService.createBudgetFromDto(budget);

            // Transaction inside period
            TransactionDTO inside = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(LocalDate.of(2025, 11, 15))
                    .description("Inside")
                    .build();
            transactionService.createTransactionFromDto(inside);

            // Transaction outside period (before)
            TransactionDTO before = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("200.00"))
                    .transactionDate(LocalDate.of(2025, 10, 15))
                    .description("Before")
                    .build();
            transactionService.createTransactionFromDto(before);

            BigDecimal spent = budgetService.calculateSpentAmount(
                    userContext.user(),
                    userContext.expenseCategory(),
                    startDate,
                    endDate
            );

            assertEquals(new BigDecimal("100.00"), spent);
        }

        @Test
        @DisplayName("Should exclude income transactions from spending")
        void shouldExcludeIncomeFromSpending() {
            LocalDate startDate = LocalDate.of(2025, 12, 1);
            LocalDate endDate = LocalDate.of(2025, 12, 31);

            // Create budget
            BudgetDTO budget = BudgetDTO.builder()
                    .userId(userContext.user().getUserId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            budgetService.createBudgetFromDto(budget);

            // Create expense transaction
            TransactionDTO expense = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("50.00"))
                    .transactionDate(LocalDate.of(2025, 12, 15))
                    .description("Expense")
                    .build();
            transactionService.createTransactionFromDto(expense);

            // Create income transaction in same category (refund scenario)
            TransactionDTO income = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.INCOME)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(LocalDate.of(2025, 12, 15))
                    .description("Refund")
                    .build();
            transactionService.createTransactionFromDto(income);

            BigDecimal spent = budgetService.calculateSpentAmount(
                    userContext.user(),
                    userContext.expenseCategory(),
                    startDate,
                    endDate
            );

            assertEquals(new BigDecimal("50.00"), spent);
        }
    }
}
