package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Category;
import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.Transaction;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.CategorySpendingDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
import com.jameselner.finance_hub.repository.IncomeSourceRepository;
import com.jameselner.finance_hub.repository.TransactionRepository;
import com.jameselner.finance_hub.repository.UserRepository;
import com.jameselner.finance_hub.util.FinancialThresholds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MonthlySummaryService Tests")
class MonthlySummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncomeSourceRepository incomeSourceRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private HousingExpenseService housingExpenseService;

    @Mock
    private DebtPaymentService debtPaymentService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private MonthlySummaryService monthlySummaryService;

    private User testUser;
    private YearMonth testMonth;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");

        testMonth = YearMonth.of(2024, 6);
    }

    @Nested
    @DisplayName("generateMonthlySummary Tests")
    class GenerateMonthlySummaryTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> monthlySummaryService.generateMonthlySummary(null, testMonth)
            );

            assertEquals("User ID must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when month is null")
        void shouldThrowExceptionWhenMonthIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> monthlySummaryService.generateMonthlySummary(1L, null)
            );

            assertEquals("Month must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> monthlySummaryService.generateMonthlySummary(999L, testMonth)
            );

            assertTrue(exception.getMessage().contains("999"));
        }

        @Test
        @DisplayName("Should calculate total expenses including all categories")
        void shouldCalculateTotalExpensesIncludingAllCategories() {
            setupDefaultMocks();

            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                    .thenReturn(new BigDecimal("500.00"));
            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("1000.00"));
            when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("200.00"));
            when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("50.00"));
            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("100.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Total = 500 + 1000 + 200 + 50 + 100 = 1850
            assertEquals(new BigDecimal("1850.00"), result.getTotalExpenses());
            assertEquals(new BigDecimal("500.00"), result.getTransactionExpenses());
        }

        @Test
        @DisplayName("Should calculate net savings correctly")
        void shouldCalculateNetSavingsCorrectly() {
            setupDefaultMocks();

            // Income: 3000, Expenses: 1850
            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(createIncomeSource(new BigDecimal("3000.00"), null)));
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                    .thenReturn(new BigDecimal("500.00"));
            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("1000.00"));
            when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("200.00"));
            when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("50.00"));
            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("100.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Net savings = 3000 - 1850 = 1150
            assertEquals(new BigDecimal("1150.00"), result.getNetSavings());
        }

        @Test
        @DisplayName("Should calculate savings rate correctly")
        void shouldCalculateSavingsRateCorrectly() {
            setupDefaultMocks();

            // Income: 2000, Net Savings: 400 -> Rate = 20%
            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(createIncomeSource(new BigDecimal("2000.00"), null)));
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                    .thenReturn(new BigDecimal("1600.00"));
            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);
            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(BigDecimal.ZERO);

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Savings rate = 400/2000 * 100 = 20%
            assertEquals(new BigDecimal("20.00"), result.getSavingsRate());
        }

        @Test
        @DisplayName("Should return zero savings rate when income is zero")
        void shouldReturnZeroSavingsRateWhenIncomeIsZero() {
            setupDefaultMocks();

            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(Collections.emptyList());

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(BigDecimal.ZERO, result.getSavingsRate());
        }

        @Test
        @DisplayName("Should use selected month year for housing ratio")
        void shouldUseSelectedMonthYearForHousingRatio() {
            setupDefaultMocks();

            YearMonth pastMonth = YearMonth.of(2023, 3);

            monthlySummaryService.generateMonthlySummary(1L, pastMonth);

            // Verify that housing ratio is calculated for 2023, not current year
            verify(housingExpenseService).calculateHousingToIncomeRatioForYear(1L, 2023);
        }

        @Test
        @DisplayName("Should add Housing category when housing costs >= 1")
        void shouldAddHousingCategoryWhenCostsExist() {
            setupDefaultMocks();

            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("1500.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertTrue(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOUSING_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should not add Housing category when housing costs < 1")
        void shouldNotAddHousingCategoryWhenCostsAreLow() {
            setupDefaultMocks();

            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("0.50"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertFalse(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOUSING_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should add Debt Payments category when payments >= 1")
        void shouldAddDebtPaymentsCategoryWhenPaymentsExist() {
            setupDefaultMocks();

            when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("300.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertTrue(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.DEBT_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should add Subscriptions category when costs >= 1")
        void shouldAddSubscriptionsCategoryWhenCostsExist() {
            setupDefaultMocks();

            when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("75.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertTrue(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.SUBSCRIPTIONS_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should add Holidays category when holiday costs >= 1")
        void shouldAddHolidaysCategoryWhenCostsExist() {
            setupDefaultMocks();

            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("500.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertTrue(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOLIDAYS_CATEGORY_ID)));
            assertEquals(new BigDecimal("500.00"), result.getHolidayCosts());
        }

        @Test
        @DisplayName("Should not add Holidays category when holiday costs < 1")
        void shouldNotAddHolidaysCategoryWhenCostsAreLow() {
            setupDefaultMocks();

            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("0.50"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertFalse(result.getTopSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOLIDAYS_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should include holiday costs in total expenses")
        void shouldIncludeHolidayCostsInTotalExpenses() {
            setupDefaultMocks();

            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                    .thenReturn(new BigDecimal("500.00"));
            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("300.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Total = 500 (transactions) + 0 (housing) + 0 (debt) + 0 (subs) + 300 (holidays) = 800
            assertEquals(new BigDecimal("800.00"), result.getTotalExpenses());
        }

        @Test
        @DisplayName("Should populate variable and fixed category lists correctly")
        void shouldPopulateVariableAndFixedCategoryListsCorrectly() {
            setupDefaultMocks();

            // Set up a transaction with a category
            Category groceries = new Category();
            groceries.setCategoryId(10L);
            groceries.setCategoryName("Groceries");
            groceries.setColorCode("#FF0000");

            Transaction t1 = createTransaction(new BigDecimal("200.00"), TransactionType.EXPENSE);
            t1.setCategory(groceries);

            when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                    eq(testUser), any(), any()))
                    .thenReturn(List.of(t1));
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                    .thenReturn(new BigDecimal("200.00"));
            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("1000.00"));
            when(holidayService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("400.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Variable categories should contain Groceries
            assertNotNull(result.getVariableSpendingCategories());
            assertTrue(result.getVariableSpendingCategories().stream()
                    .anyMatch(c -> "Groceries".equals(c.getCategoryName())));

            // Fixed categories should contain Housing and Holidays
            assertNotNull(result.getFixedCostCategories());
            assertTrue(result.getFixedCostCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOUSING_CATEGORY_ID)));
            assertTrue(result.getFixedCostCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOLIDAYS_CATEGORY_ID)));

            // Variable categories should NOT contain synthetic categories
            assertFalse(result.getVariableSpendingCategories().stream()
                    .anyMatch(c -> c.getCategoryId().equals(FinancialThresholds.HOUSING_CATEGORY_ID)));
        }

        @Test
        @DisplayName("Should sort spending categories by total spent descending")
        void shouldSortSpendingCategoriesByTotalSpentDescending() {
            setupDefaultMocks();

            when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("1000.00"));
            when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                    .thenReturn(new BigDecimal("500.00"));
            when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(new BigDecimal("100.00"));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            List<BigDecimal> amounts = result.getTopSpendingCategories().stream()
                    .map(CategorySpendingDTO::getTotalSpent)
                    .toList();

            // Verify descending order
            for (int i = 0; i < amounts.size() - 1; i++) {
                assertTrue(amounts.get(i).compareTo(amounts.get(i + 1)) >= 0);
            }
        }
    }

    @Nested
    @DisplayName("Income Calculation Tests")
    class IncomeCalculationTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            setupDefaultMocks();
        }

        @Test
        @DisplayName("Should use net amount when available")
        void shouldUseNetAmountWhenAvailable() {
            IncomeSource source = createIncomeSource(
                    new BigDecimal("3000.00"),  // gross
                    new BigDecimal("2400.00")   // net
            );
            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(source));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(new BigDecimal("2400.00"), result.getTotalIncome());
        }

        @Test
        @DisplayName("Should use gross amount when net is null")
        void shouldUseGrossAmountWhenNetIsNull() {
            IncomeSource source = createIncomeSource(
                    new BigDecimal("3000.00"),  // gross
                    null                        // net
            );
            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(source));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(new BigDecimal("3000.00"), result.getTotalIncome());
        }

        @Test
        @DisplayName("Should return zero when both net and gross are null")
        void shouldReturnZeroWhenBothNetAndGrossAreNull() {
            IncomeSource source = createIncomeSource(null, null);
            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(source));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(BigDecimal.ZERO, result.getTotalIncome());
        }

        @Test
        @DisplayName("Should sum multiple income sources correctly")
        void shouldSumMultipleIncomeSourcesCorrectly() {
            IncomeSource salary = createIncomeSource(new BigDecimal("4000.00"), new BigDecimal("3200.00"));
            IncomeSource freelance = createIncomeSource(new BigDecimal("1000.00"), null);
            IncomeSource bonus = createIncomeSource(new BigDecimal("500.00"), new BigDecimal("400.00"));

            when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                    .thenReturn(List.of(salary, freelance, bonus));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // 3200 (net) + 1000 (gross) + 400 (net) = 4600
            assertEquals(new BigDecimal("4600.00"), result.getTotalIncome());
        }
    }

    @Nested
    @DisplayName("Budget Performance Tests")
    class BudgetPerformanceTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            setupDefaultMocks();
        }

        @Test
        @DisplayName("Should calculate budget utilization correctly")
        void shouldCalculateBudgetUtilizationCorrectly() {
            BudgetDTO budget1 = BudgetDTO.builder()
                    .budgetId(1L)
                    .amount(new BigDecimal("500.00"))
                    .spent(new BigDecimal("400.00"))
                    .overage(BigDecimal.ZERO)
                    .categoryId(1L)
                    .build();
            BudgetDTO budget2 = BudgetDTO.builder()
                    .budgetId(2L)
                    .amount(new BigDecimal("300.00"))
                    .spent(new BigDecimal("300.00"))
                    .overage(BigDecimal.ZERO)
                    .categoryId(2L)
                    .build();

            when(budgetService.findByUserIdAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of(budget1, budget2));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Total budgeted: 800, Total spent: 700 -> Utilization: 87.5%
            assertEquals(new BigDecimal("800.00"), result.getTotalBudgeted());
            assertEquals(new BigDecimal("700.00"), result.getTotalSpent());
            assertEquals(new BigDecimal("87.50"), result.getBudgetUtilization());
        }

        @Test
        @DisplayName("Should count budgets over and on track correctly")
        void shouldCountBudgetsOverAndOnTrackCorrectly() {
            BudgetDTO onTrack = BudgetDTO.builder()
                    .budgetId(1L)
                    .amount(new BigDecimal("500.00"))
                    .spent(new BigDecimal("400.00"))
                    .overage(BigDecimal.ZERO)
                    .categoryId(1L)
                    .build();
            BudgetDTO overBudget = BudgetDTO.builder()
                    .budgetId(2L)
                    .amount(new BigDecimal("200.00"))
                    .spent(new BigDecimal("250.00"))
                    .overage(new BigDecimal("50.00"))
                    .categoryId(2L)
                    .build();

            when(budgetService.findByUserIdAndDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of(onTrack, overBudget));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(1, result.getBudgetsOnTrackCount());
            assertEquals(1, result.getBudgetsOverCount());
        }
    }

    @Nested
    @DisplayName("Month Comparison Tests")
    class MonthComparisonTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        }

        @Test
        @DisplayName("Should calculate income change correctly")
        void shouldCalculateIncomeChangeCorrectly() {
            // Current month: 3000, Previous month: 2500
            setupMocksForComparison(
                    new BigDecimal("3000.00"), new BigDecimal("2500.00"),
                    BigDecimal.ZERO, BigDecimal.ZERO
            );

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(new BigDecimal("500.00"), result.getMonthComparison().getIncomeChange());
            assertEquals(new BigDecimal("20.00"), result.getMonthComparison().getIncomeChangePercent());
        }

        @Test
        @DisplayName("Should handle zero previous income gracefully")
        void shouldHandleZeroPreviousIncomeGracefully() {
            // Current month: 3000, Previous month: 0
            setupMocksForComparison(
                    new BigDecimal("3000.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Should return zero percent change when previous is zero
            assertEquals(BigDecimal.ZERO, result.getMonthComparison().getIncomeChangePercent());
        }
    }

    @Nested
    @DisplayName("Transaction Statistics Tests")
    class TransactionStatisticsTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            setupDefaultMocks();
        }

        @Test
        @DisplayName("Should calculate average transaction size for expenses only")
        void shouldCalculateAverageTransactionSizeCorrectly() {
            Transaction t1 = createTransaction(new BigDecimal("100.00"), TransactionType.EXPENSE);
            Transaction t2 = createTransaction(new BigDecimal("200.00"), TransactionType.EXPENSE);
            Transaction t3 = createTransaction(new BigDecimal("300.00"), TransactionType.INCOME);

            when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                    eq(testUser), any(), any()))
                    .thenReturn(List.of(t1, t2, t3));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            // Average of expenses only = (100 + 200) / 2 = 150
            assertEquals(new BigDecimal("150.00"), result.getAverageTransactionSize());
            assertEquals(3, result.getTransactionCount());
        }

        @Test
        @DisplayName("Should find largest expense correctly")
        void shouldFindLargestExpenseCorrectly() {
            Category category = new Category();
            category.setCategoryId(1L);
            category.setCategoryName("Shopping");

            Transaction t1 = createTransaction(new BigDecimal("100.00"), TransactionType.EXPENSE);
            Transaction t2 = createTransaction(new BigDecimal("500.00"), TransactionType.EXPENSE);
            t2.setCategory(category);
            Transaction t3 = createTransaction(new BigDecimal("1000.00"), TransactionType.INCOME);

            when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                    eq(testUser), any(), any()))
                    .thenReturn(List.of(t1, t2, t3));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(new BigDecimal("500.00"), result.getLargestExpense());
            assertEquals("Shopping", result.getLargestExpenseCategory());
        }

        @Test
        @DisplayName("Should return zero for largest expense when no expenses")
        void shouldReturnZeroForLargestExpenseWhenNoExpenses() {
            Transaction income = createTransaction(new BigDecimal("1000.00"), TransactionType.INCOME);

            when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                    eq(testUser), any(), any()))
                    .thenReturn(List.of(income));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals(BigDecimal.ZERO, result.getLargestExpense());
            assertEquals("N/A", result.getLargestExpenseCategory());
        }

        @Test
        @DisplayName("Should return N/A category when largest expense has no category")
        void shouldReturnNaCategoryWhenLargestExpenseHasNoCategory() {
            Transaction expense = createTransaction(new BigDecimal("500.00"), TransactionType.EXPENSE);
            expense.setCategory(null);

            when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                    eq(testUser), any(), any()))
                    .thenReturn(List.of(expense));

            MonthlySummaryDTO result = monthlySummaryService.generateMonthlySummary(1L, testMonth);

            assertEquals("N/A", result.getLargestExpenseCategory());
        }
    }

    @Nested
    @DisplayName("getEarliestAvailableMonth Tests")
    class GetEarliestAvailableMonthTests {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        }

        @Test
        @DisplayName("Should return earliest transaction month")
        void shouldReturnEarliestTransactionMonth() {
            when(transactionRepository.findEarliestTransactionDateByUser(testUser))
                    .thenReturn(Optional.of(LocalDate.of(2023, 3, 15)));

            YearMonth result = monthlySummaryService.getEarliestAvailableMonth(1L);

            assertEquals(YearMonth.of(2023, 3), result);
        }

        @Test
        @DisplayName("Should default to current month when no transactions")
        void shouldDefaultToCurrentMonthWhenNoTransactions() {
            when(transactionRepository.findEarliestTransactionDateByUser(testUser))
                    .thenReturn(Optional.empty());

            YearMonth result = monthlySummaryService.getEarliestAvailableMonth(1L);

            assertEquals(YearMonth.now(), result);
        }

        @Test
        @DisplayName("Should throw on null userId")
        void shouldThrowOnNullUserId() {
            assertThrows(NullPointerException.class,
                    () -> monthlySummaryService.getEarliestAvailableMonth(null));
        }
    }

    // Helper methods

    private void setupDefaultMocks() {
        when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), any(), any()))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(testUser), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(holidayService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(budgetService.findByUserIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(housingExpenseService.calculateHousingToIncomeRatioForYear(eq(1L), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                eq(testUser), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    private void setupMocksForComparison(
            BigDecimal currentIncome, BigDecimal previousIncome,
            BigDecimal currentExpenses, BigDecimal previousExpenses) {

        LocalDate currentStart = testMonth.atDay(1);
        LocalDate currentEnd = testMonth.atEndOfMonth();
        YearMonth previousMonth = testMonth.minusMonths(1);
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        // Current month income
        when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), eq(currentStart), eq(currentEnd)))
                .thenReturn(currentIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        List.of(createIncomeSource(currentIncome, null)) : Collections.emptyList());

        // Previous month income
        when(incomeSourceRepository.findByUserAndMonthRange(eq(testUser), eq(previousStart), eq(previousEnd)))
                .thenReturn(previousIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        List.of(createIncomeSource(previousIncome, null)) : Collections.emptyList());

        // Current month expenses
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(testUser), eq(TransactionType.EXPENSE), eq(currentStart), eq(currentEnd)))
                .thenReturn(currentExpenses);

        // Previous month expenses
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(testUser), eq(TransactionType.EXPENSE), eq(previousStart), eq(previousEnd)))
                .thenReturn(previousExpenses);

        // Default zero for other expense types
        when(housingExpenseService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(debtPaymentService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(subscriptionService.calculateTotalForMonth(eq(1L), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(holidayService.calculateTotalForMonth(eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(budgetService.findByUserIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(housingExpenseService.calculateHousingToIncomeRatioForYear(eq(1L), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByAccountUserAndTransactionDateBetween(
                eq(testUser), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    private IncomeSource createIncomeSource(BigDecimal grossAmount, BigDecimal netAmount) {
        IncomeSource source = new IncomeSource();
        source.setUser(testUser);
        source.setGrossAmount(grossAmount);
        source.setNetAmount(netAmount);
        source.setStartDate(testMonth.atDay(15));
        return source;
    }

    private Transaction createTransaction(BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setTransactionDate(testMonth.atDay(15));
        return transaction;
    }
}
