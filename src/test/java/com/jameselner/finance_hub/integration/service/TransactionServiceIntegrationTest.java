package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.TransactionDTO;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory.TestUserContext;
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

@DisplayName("TransactionService Integration Tests")
class TransactionServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestFixtureFactory fixtureFactory;

    private TestUserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = fixtureFactory.createBasicUser("txn-test@example.com");
    }

    @Nested
    @DisplayName("Transaction CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should create and retrieve transaction")
        void shouldCreateAndRetrieveTransaction() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("75.50"))
                    .transactionDate(LocalDate.now())
                    .description("Grocery shopping")
                    .build();

            TransactionDTO created = transactionService.createTransactionFromDto(dto);

            assertNotNull(created.getTransactionId());
            assertEquals(new BigDecimal("75.50"), created.getAmount());

            Optional<TransactionDTO> retrieved = transactionService.findByIdAsDto(created.getTransactionId());
            assertTrue(retrieved.isPresent());
            assertEquals("Grocery shopping", retrieved.get().getDescription());
        }

        @Test
        @DisplayName("Should create income transaction")
        void shouldCreateIncomeTransaction() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.incomeCategory().getCategoryId())
                    .transactionType(TransactionType.INCOME)
                    .amount(new BigDecimal("2500.00"))
                    .transactionDate(LocalDate.now())
                    .description("Monthly salary")
                    .build();

            TransactionDTO created = transactionService.createTransactionFromDto(dto);

            assertNotNull(created.getTransactionId());
            assertEquals(TransactionType.INCOME, created.getTransactionType());
            assertEquals(new BigDecimal("2500.00"), created.getAmount());
        }

        @Test
        @DisplayName("Should update transaction")
        void shouldUpdateTransaction() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(LocalDate.now())
                    .description("Original description")
                    .build();
            TransactionDTO created = transactionService.createTransactionFromDto(dto);

            created.setAmount(new BigDecimal("150.00"));
            created.setDescription("Updated description");
            TransactionDTO updated = transactionService.updateTransactionFromDto(created);

            assertEquals(new BigDecimal("150.00"), updated.getAmount());
            assertEquals("Updated description", updated.getDescription());
        }

        @Test
        @DisplayName("Should delete transaction")
        void shouldDeleteTransaction() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("50.00"))
                    .transactionDate(LocalDate.now())
                    .description("To be deleted")
                    .build();
            TransactionDTO created = transactionService.createTransactionFromDto(dto);
            Long transactionId = created.getTransactionId();

            transactionService.deleteById(transactionId);

            assertTrue(transactionService.findByIdAsDto(transactionId).isEmpty());
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(BigDecimal.ZERO)
                    .transactionDate(LocalDate.now())
                    .description("Invalid")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(dto));
        }

        @Test
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            TransactionDTO dto = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("-50.00"))
                    .transactionDate(LocalDate.now())
                    .description("Invalid")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.createTransactionFromDto(dto));
        }
    }

    @Nested
    @DisplayName("Transaction Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find all transactions")
        void shouldFindAllTransactions() {
            for (int i = 0; i < 5; i++) {
                TransactionDTO dto = TransactionDTO.builder()
                        .accountId(userContext.account().getAccountId())
                        .categoryId(userContext.expenseCategory().getCategoryId())
                        .transactionType(TransactionType.EXPENSE)
                        .amount(new BigDecimal("100.00"))
                        .transactionDate(LocalDate.now().minusDays(i))
                        .description("Transaction " + i)
                        .build();
                transactionService.createTransactionFromDto(dto);
            }

            List<TransactionDTO> all = transactionService.findAllAsDto();

            assertEquals(5, all.size());
        }

        @Test
        @DisplayName("Should calculate sum by user, type, and date range")
        void shouldCalculateSumByUserTypeAndDateRange() {
            for (int i = 0; i < 5; i++) {
                TransactionDTO dto = TransactionDTO.builder()
                        .accountId(userContext.account().getAccountId())
                        .categoryId(userContext.expenseCategory().getCategoryId())
                        .transactionType(TransactionType.EXPENSE)
                        .amount(new BigDecimal("100.00"))
                        .transactionDate(LocalDate.now().minusDays(i))
                        .description("Transaction " + i)
                        .build();
                transactionService.createTransactionFromDto(dto);
            }

            BigDecimal sum = transactionService.sumByUserAndTypeAndDateRange(
                    userContext.user(),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(10),
                    LocalDate.now()
            );

            assertEquals(new BigDecimal("500.00"), sum);
        }

        @Test
        @DisplayName("Should return zero for empty date range")
        void shouldReturnZeroForEmptyDateRange() {
            BigDecimal sum = transactionService.sumByUserAndTypeAndDateRange(
                    userContext.user(),
                    TransactionType.EXPENSE,
                    LocalDate.now().plusDays(100),
                    LocalDate.now().plusDays(200)
            );

            assertEquals(BigDecimal.ZERO, sum);
        }

        @Test
        @DisplayName("Should sum only specified transaction type")
        void shouldSumOnlySpecifiedType() {
            // Create expense
            TransactionDTO expense = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.expenseCategory().getCategoryId())
                    .transactionType(TransactionType.EXPENSE)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(LocalDate.now())
                    .description("Expense")
                    .build();
            transactionService.createTransactionFromDto(expense);

            // Create income
            TransactionDTO income = TransactionDTO.builder()
                    .accountId(userContext.account().getAccountId())
                    .categoryId(userContext.incomeCategory().getCategoryId())
                    .transactionType(TransactionType.INCOME)
                    .amount(new BigDecimal("500.00"))
                    .transactionDate(LocalDate.now())
                    .description("Income")
                    .build();
            transactionService.createTransactionFromDto(income);

            BigDecimal expenseSum = transactionService.sumByUserAndTypeAndDateRange(
                    userContext.user(),
                    TransactionType.EXPENSE,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1)
            );

            BigDecimal incomeSum = transactionService.sumByUserAndTypeAndDateRange(
                    userContext.user(),
                    TransactionType.INCOME,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1)
            );

            assertEquals(new BigDecimal("100.00"), expenseSum);
            assertEquals(new BigDecimal("500.00"), incomeSum);
        }
    }
}
