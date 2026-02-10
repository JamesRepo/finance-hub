package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.PeriodType;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO;
import com.jameselner.finance_hub.dto.AnnualProjectionDTO.CategoryProjection;
import com.jameselner.finance_hub.dto.BudgetDTO;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.dto.HolidayDTO;
import com.jameselner.finance_hub.dto.MonthlySummaryDTO;
import com.jameselner.finance_hub.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AnnualProjectionService Tests")
class AnnualProjectionServiceTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private HousingExpenseService housingExpenseService;

    @Mock
    private DebtService debtService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private HolidayService holidayService;

    @Mock
    private MonthlySummaryService monthlySummaryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnualProjectionService annualProjectionService;

    private User testUser;
    private static final Long USER_ID = 1L;
    private static final int TEST_YEAR = 2025;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail("test@example.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("Budget Annualization Tests")
    class BudgetAnnualizationTests {

        @Test
        @DisplayName("Should annualize MONTHLY budget for full year")
        void shouldAnnualizeMonthlyBudgetForFullYear() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(1L)
                    .categoryName("Groceries")
                    .categoryColorCode("#FF0000")
                    .amount(new BigDecimal("500.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            assertEquals(0, new BigDecimal("6000.00").compareTo(result),
                    "Monthly budget of 500 × 12 months = 6000");
        }

        @Test
        @DisplayName("Should annualize WEEKLY budget for full year")
        void shouldAnnualizeWeeklyBudgetForFullYear() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(2L)
                    .categoryName("Entertainment")
                    .amount(new BigDecimal("100.00"))
                    .periodType(PeriodType.WEEKLY)
                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            // 365 days / 7 ≈ 52.14 weeks × 100 = ~5214
            assertTrue(result.compareTo(new BigDecimal("5200")) > 0);
            assertTrue(result.compareTo(new BigDecimal("5300")) < 0);
        }

        @Test
        @DisplayName("Should annualize QUARTERLY budget for full year")
        void shouldAnnualizeQuarterlyBudgetForFullYear() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(3L)
                    .categoryName("Insurance")
                    .amount(new BigDecimal("300.00"))
                    .periodType(PeriodType.QUARTERLY)
                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            assertEquals(0, new BigDecimal("1200.00").compareTo(result.setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Quarterly budget of 300 × (12/3) = 1200");
        }

        @Test
        @DisplayName("Should annualize YEARLY budget for full year")
        void shouldAnnualizeYearlyBudgetForFullYear() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(4L)
                    .categoryName("Car Insurance")
                    .amount(new BigDecimal("1200.00"))
                    .periodType(PeriodType.YEARLY)
                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            assertEquals(0, new BigDecimal("1200.00").compareTo(result.setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Yearly budget of 1200 for full year = 1200");
        }

        @Test
        @DisplayName("Should handle partial-year budget (Jan-Jun)")
        void shouldHandlePartialYearBudget() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(1L)
                    .categoryName("Gym")
                    .amount(new BigDecimal("50.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                    .endDate(LocalDate.of(TEST_YEAR, 6, 30))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            assertEquals(0, new BigDecimal("300.00").compareTo(result),
                    "Monthly budget of 50 × 6 months (Jan-Jun) = 300");
        }

        @Test
        @DisplayName("Should return zero for budget outside target year")
        void shouldReturnZeroForBudgetOutsideTargetYear() {
            BudgetDTO budget = BudgetDTO.builder()
                    .categoryId(1L)
                    .categoryName("Old Budget")
                    .amount(new BigDecimal("100.00"))
                    .periodType(PeriodType.MONTHLY)
                    .startDate(LocalDate.of(2023, 1, 1))
                    .endDate(LocalDate.of(2023, 12, 31))
                    .build();

            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            BigDecimal result = annualProjectionService.annualizeBudget(budget, yearStart, yearEnd);
            assertEquals(0, BigDecimal.ZERO.compareTo(result),
                    "Budget outside target year should return zero");
        }
    }

    @Nested
    @DisplayName("Pseudo-Category Projection Tests")
    class PseudoCategoryProjectionTests {

        @Test
        @DisplayName("Should project housing costs from current month × 12")
        void shouldProjectHousingCosts() {
            setupDefaultMocks();

            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);
            assertEquals(0, new BigDecimal("12000.00").compareTo(projection.getHousingCosts()));
        }

        @Test
        @DisplayName("Should project subscription costs using current month total × 12")
        void shouldProjectSubscriptionCosts() {
            // Current month total is £50, so annual = £600
            when(subscriptionService.calculateCurrentMonthTotal(USER_ID))
                    .thenReturn(new BigDecimal("50.00"));

            BigDecimal result = annualProjectionService.projectSubscriptionCosts(USER_ID);
            assertEquals(0, new BigDecimal("600.00").compareTo(result));
        }

        @Test
        @DisplayName("Should project debt payments from minimum payment × 12 for active debts")
        void shouldProjectDebtPayments() {
            List<DebtDTO> debts = List.of(
                    DebtDTO.builder()
                            .active(true)
                            .currentBalance(new BigDecimal("5000.00"))
                            .minimumPayment(new BigDecimal("150.00"))
                            .build(),
                    DebtDTO.builder()
                            .active(true)
                            .currentBalance(new BigDecimal("10000.00"))
                            .minimumPayment(new BigDecimal("300.00"))
                            .build(),
                    // Inactive debt - should be excluded
                    DebtDTO.builder()
                            .active(false)
                            .currentBalance(new BigDecimal("2000.00"))
                            .minimumPayment(new BigDecimal("50.00"))
                            .build(),
                    // Zero balance debt - should be excluded
                    DebtDTO.builder()
                            .active(true)
                            .currentBalance(BigDecimal.ZERO)
                            .minimumPayment(new BigDecimal("100.00"))
                            .build()
            );
            when(debtService.findAllByUserAsDto(testUser)).thenReturn(debts);

            BigDecimal result = annualProjectionService.projectDebtPayments(testUser);
            // (150 + 300) × 12 = 5400
            assertEquals(0, new BigDecimal("5400.00").compareTo(result));
        }

        @Test
        @DisplayName("Should project holiday costs from holiday budgets overlapping target year")
        void shouldProjectHolidayCosts() {
            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            List<HolidayDTO> holidays = List.of(
                    HolidayDTO.builder()
                            .budget(new BigDecimal("2000.00"))
                            .startDate(LocalDate.of(TEST_YEAR, 6, 15))
                            .endDate(LocalDate.of(TEST_YEAR, 6, 28))
                            .isActive(true)
                            .build(),
                    HolidayDTO.builder()
                            .budget(new BigDecimal("500.00"))
                            .startDate(LocalDate.of(TEST_YEAR, 12, 20))
                            .endDate(LocalDate.of(TEST_YEAR + 1, 1, 5))
                            .isActive(true)
                            .build(),
                    // Holiday outside the year - should be excluded
                    HolidayDTO.builder()
                            .budget(new BigDecimal("3000.00"))
                            .startDate(LocalDate.of(TEST_YEAR + 1, 3, 1))
                            .endDate(LocalDate.of(TEST_YEAR + 1, 3, 15))
                            .isActive(true)
                            .build()
            );
            when(holidayService.findActiveByUserIdAsDto(USER_ID)).thenReturn(holidays);

            BigDecimal result = annualProjectionService.projectHolidayCosts(USER_ID, yearStart, yearEnd);
            // 2000 + 500 = 2500 (third holiday is after year end)
            assertEquals(0, new BigDecimal("2500.00").compareTo(result));
        }
    }

    @Nested
    @DisplayName("Income Projection Tests")
    class IncomeProjectionTests {

        @Test
        @DisplayName("Should project income from last month actual × 12")
        void shouldProjectIncomeFromLastMonthActual() {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                    .thenReturn(MonthlySummaryDTO.builder()
                            .totalIncome(new BigDecimal("4000.00"))
                            .build());

            BigDecimal result = annualProjectionService.projectIncome(USER_ID);
            assertEquals(0, new BigDecimal("48000.00").compareTo(result));
        }
    }

    @Nested
    @DisplayName("Full Projection Tests")
    class FullProjectionTests {

        @Test
        @DisplayName("Should aggregate all expense sources into projectedExpenses")
        void shouldAggregateAllExpenseSources() {
            setupDefaultMocks();

            // Income: last month = 5000, projected = 60000
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                    .thenReturn(MonthlySummaryDTO.builder()
                            .totalIncome(new BigDecimal("5000.00"))
                            .build());

            // Budgets: monthly 500 × 12 = 6000
            when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            BudgetDTO.builder()
                                    .categoryId(1L)
                                    .categoryName("Groceries")
                                    .categoryColorCode("#FF0000")
                                    .amount(new BigDecimal("500.00"))
                                    .periodType(PeriodType.MONTHLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build()
                    ));

            // Housing: 12000
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));

            // Subscriptions: current month 50, annual = 600
            when(subscriptionService.calculateCurrentMonthTotal(USER_ID))
                    .thenReturn(new BigDecimal("50.00"));

            // Debt: 150 × 12 = 1800
            when(debtService.findAllByUserAsDto(testUser)).thenReturn(List.of(
                    DebtDTO.builder()
                            .active(true)
                            .currentBalance(new BigDecimal("5000.00"))
                            .minimumPayment(new BigDecimal("150.00"))
                            .build()
            ));

            // Holidays: 2000
            when(holidayService.findActiveByUserIdAsDto(USER_ID)).thenReturn(List.of(
                    HolidayDTO.builder()
                            .budget(new BigDecimal("2000.00"))
                            .startDate(LocalDate.of(TEST_YEAR, 7, 1))
                            .endDate(LocalDate.of(TEST_YEAR, 7, 14))
                            .isActive(true)
                            .build()
            ));

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);

            // Expected total: 6000 + 12000 + 600 + 1800 + 2000 = 22400
            assertEquals(0, new BigDecimal("22400.00").compareTo(projection.getProjectedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Total expenses should be sum of all sources");
            assertEquals(0, new BigDecimal("60000.00").compareTo(projection.getProjectedIncome()));

            // Net savings = 60000 - 22400 = 37600
            assertEquals(0, new BigDecimal("37600.00").compareTo(projection.getProjectedNetSavings().setScale(2, java.math.RoundingMode.HALF_UP)));

            // Savings rate = (37600 / 60000) × 100 ≈ 62.67%
            assertTrue(projection.getProjectedSavingsRate().compareTo(BigDecimal.valueOf(62)) > 0);
            assertTrue(projection.getProjectedSavingsRate().compareTo(BigDecimal.valueOf(63)) < 0);
        }

        @Test
        @DisplayName("Should handle projection with no budgets")
        void shouldHandleNoBudgets() {
            setupDefaultMocks();

            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                    .thenReturn(MonthlySummaryDTO.builder()
                            .totalIncome(new BigDecimal("3333.33"))
                            .build());
            when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);

            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getBudgetedExpenses()));
            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getProjectedExpenses()));
            // 3333.33 × 12 = 39999.96
            assertEquals(0, new BigDecimal("39999.96").compareTo(projection.getProjectedNetSavings()));
        }

        @Test
        @DisplayName("Should handle projection with no income")
        void shouldHandleNoIncome() {
            setupDefaultMocks();

            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                    .thenReturn(MonthlySummaryDTO.builder()
                            .totalIncome(BigDecimal.ZERO)
                            .build());
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);

            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getProjectedIncome()));
            assertEquals(0, new BigDecimal("-12000.00").compareTo(projection.getProjectedNetSavings()));
            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getProjectedSavingsRate()));
        }

        @Test
        @DisplayName("Should handle projection with no debts")
        void shouldHandleNoDebts() {
            setupDefaultMocks();
            when(debtService.findAllByUserAsDto(testUser)).thenReturn(Collections.emptyList());

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);

            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getDebtPayments()));
        }

        @Test
        @DisplayName("Should include category projections sorted by amount descending")
        void shouldIncludeSortedCategoryProjections() {
            setupDefaultMocks();

            when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            BudgetDTO.builder()
                                    .categoryId(1L)
                                    .categoryName("Groceries")
                                    .categoryColorCode("#FF0000")
                                    .amount(new BigDecimal("800.00"))
                                    .periodType(PeriodType.MONTHLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build(),
                            BudgetDTO.builder()
                                    .categoryId(2L)
                                    .categoryName("Entertainment")
                                    .categoryColorCode("#00FF00")
                                    .amount(new BigDecimal("200.00"))
                                    .periodType(PeriodType.MONTHLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build()
                    ));

            // Housing costs larger than entertainment but smaller than groceries
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("6000.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);

            assertNotNull(projection.getCategoryProjections());
            assertFalse(projection.getCategoryProjections().isEmpty());

            // Verify sorted by amount descending
            for (int i = 0; i < projection.getCategoryProjections().size() - 1; i++) {
                assertTrue(projection.getCategoryProjections().get(i).getAnnualAmount()
                        .compareTo(projection.getCategoryProjections().get(i + 1).getAnnualAmount()) >= 0,
                        "Categories should be sorted by amount descending");
            }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception for null userId")
        void shouldThrowForNullUserId() {
            assertThrows(NullPointerException.class,
                    () -> annualProjectionService.generateProjection(null, TEST_YEAR));
        }

        @Test
        @DisplayName("Should throw exception for null year")
        void shouldThrowForNullYear() {
            assertThrows(NullPointerException.class,
                    () -> annualProjectionService.generateProjection(USER_ID, null));
        }
    }

    @Nested
    @DisplayName("Budget Annualization Map Tests")
    class BudgetAnnualizationMapTests {

        @Test
        @DisplayName("Should exclude synthetic category budgets to avoid double-counting")
        void shouldExcludeSyntheticCategoryBudgets() {
            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            List<BudgetDTO> budgets = List.of(
                    BudgetDTO.builder()
                            .categoryId(1L)
                            .categoryName("Groceries")
                            .categoryColorCode("#FF0000")
                            .amount(new BigDecimal("500.00"))
                            .periodType(PeriodType.MONTHLY)
                            .startDate(yearStart)
                            .endDate(yearEnd)
                            .build(),
                    // "Holidays" budget should be excluded — projected separately
                    BudgetDTO.builder()
                            .categoryId(10L)
                            .categoryName("Holidays")
                            .categoryColorCode("#4DB6AC")
                            .amount(new BigDecimal("200.00"))
                            .periodType(PeriodType.MONTHLY)
                            .startDate(yearStart)
                            .endDate(yearEnd)
                            .build(),
                    // "Subscriptions" budget should be excluded — projected separately
                    BudgetDTO.builder()
                            .categoryId(11L)
                            .categoryName("Subscriptions")
                            .categoryColorCode("#9575CD")
                            .amount(new BigDecimal("100.00"))
                            .periodType(PeriodType.MONTHLY)
                            .startDate(yearStart)
                            .endDate(yearEnd)
                            .build()
            );

            Map<String, CategoryProjection> result = annualProjectionService.annualizeBudgets(budgets, yearStart, yearEnd);

            assertEquals(1, result.size(), "Should only include non-synthetic categories");
            assertNotNull(result.get("Groceries"));
            assertNull(result.get("Holidays"), "Holidays budget should be excluded");
            assertNull(result.get("Subscriptions"), "Subscriptions budget should be excluded");
            assertEquals(0, new BigDecimal("6000.00").compareTo(result.get("Groceries").getAnnualAmount()));
        }

        @Test
        @DisplayName("Should group budgets by category name")
        void shouldGroupBudgetsByCategory() {
            LocalDate yearStart = LocalDate.of(TEST_YEAR, 1, 1);
            LocalDate yearEnd = LocalDate.of(TEST_YEAR, 12, 31);

            List<BudgetDTO> budgets = List.of(
                    BudgetDTO.builder()
                            .categoryId(1L)
                            .categoryName("Groceries")
                            .categoryColorCode("#FF0000")
                            .amount(new BigDecimal("500.00"))
                            .periodType(PeriodType.MONTHLY)
                            .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                            .endDate(LocalDate.of(TEST_YEAR, 6, 30))
                            .build(),
                    BudgetDTO.builder()
                            .categoryId(1L)
                            .categoryName("Groceries")
                            .categoryColorCode("#FF0000")
                            .amount(new BigDecimal("600.00"))
                            .periodType(PeriodType.MONTHLY)
                            .startDate(LocalDate.of(TEST_YEAR, 7, 1))
                            .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                            .build()
            );

            Map<String, CategoryProjection> result = annualProjectionService.annualizeBudgets(budgets, yearStart, yearEnd);

            assertEquals(1, result.size(), "Should combine budgets for same category");
            CategoryProjection groceries = result.get("Groceries");
            assertNotNull(groceries);
            // First half: 500 × 6 = 3000, Second half: 600 × 6 = 3600, Total = 6600
            assertEquals(0, new BigDecimal("6600.00").compareTo(groceries.getAnnualAmount()));
        }
    }

    /**
     * Setup default mocks that return empty/zero values for all services.
     */
    private void setupDefaultMocks() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                .thenReturn(MonthlySummaryDTO.builder()
                        .totalIncome(BigDecimal.ZERO)
                        .build());
        when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                .thenReturn(BigDecimal.ZERO);
        when(subscriptionService.calculateCurrentMonthTotal(USER_ID))
                .thenReturn(BigDecimal.ZERO);
        when(debtService.findAllByUserAsDto(testUser))
                .thenReturn(Collections.emptyList());
        when(holidayService.findActiveByUserIdAsDto(USER_ID))
                .thenReturn(Collections.emptyList());
    }
}
