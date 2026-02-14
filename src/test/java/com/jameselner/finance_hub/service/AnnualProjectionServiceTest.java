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

    @Nested
    @DisplayName("generateProjectionWithOverrides Tests")
    class GenerateProjectionWithOverridesTests {

        @Test
        @DisplayName("Should throw exception for null userId")
        void shouldThrowForNullUserId() {
            assertThrows(NullPointerException.class,
                    () -> annualProjectionService.generateProjectionWithOverrides(
                            null, TEST_YEAR, Map.of(), null, null, null, null, null));
        }

        @Test
        @DisplayName("Should throw exception for null year")
        void shouldThrowForNullYear() {
            assertThrows(NullPointerException.class,
                    () -> annualProjectionService.generateProjectionWithOverrides(
                            USER_ID, null, Map.of(), null, null, null, null, null));
        }

        @Test
        @DisplayName("Should throw exception for null budgetOverrides map")
        void shouldThrowForNullBudgetOverrides() {
            assertThrows(NullPointerException.class,
                    () -> annualProjectionService.generateProjectionWithOverrides(
                            USER_ID, TEST_YEAR, null, null, null, null, null, null));
        }

        @Test
        @DisplayName("Should produce same results as generateProjection when all overrides are null/empty")
        void shouldMatchBaseProjectionWithNoOverrides() {
            setupDefaultMocks();

            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            when(monthlySummaryService.generateMonthlySummary(USER_ID, lastMonth))
                    .thenReturn(MonthlySummaryDTO.builder()
                            .totalIncome(new BigDecimal("4000.00"))
                            .build());
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));
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

            AnnualProjectionDTO base = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);
            AnnualProjectionDTO withOverrides = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(), null, null, null, null, null);

            assertEquals(0, base.getProjectedIncome().compareTo(withOverrides.getProjectedIncome()));
            assertEquals(0, base.getProjectedExpenses().compareTo(withOverrides.getProjectedExpenses()));
            assertEquals(0, base.getProjectedNetSavings().compareTo(withOverrides.getProjectedNetSavings()));
            assertEquals(0, base.getHousingCosts().compareTo(withOverrides.getHousingCosts()));
            assertEquals(0, base.getDebtPayments().compareTo(withOverrides.getDebtPayments()));
            assertEquals(0, base.getSubscriptionCosts().compareTo(withOverrides.getSubscriptionCosts()));
            assertEquals(0, base.getHolidayCosts().compareTo(withOverrides.getHolidayCosts()));
            assertEquals(0, base.getBudgetedExpenses().compareTo(withOverrides.getBudgetedExpenses()));
        }

        @Test
        @DisplayName("Should override income when incomeMonthlyOverride is provided")
        void shouldOverrideIncome() {
            setupDefaultMocks();

            // Real income would be 0 from default mocks
            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("5000.00"), // monthly income override
                    null, null, null, null);

            // 5000 × 12 = 60000
            assertEquals(0, new BigDecimal("60000.00").compareTo(projection.getProjectedIncome()),
                    "Income should be overridden to 5000/mo × 12 = 60000");
        }

        @Test
        @DisplayName("Should not call projectIncome when income override is provided")
        void shouldNotCallProjectIncomeWhenOverridden() {
            setupDefaultMocks();

            annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("3000.00"), null, null, null, null);

            // monthlySummaryService should not be called for income projection
            // (it's called by setupDefaultMocks, but only once for the mock setup,
            // not again during projection)
            verify(monthlySummaryService, never()).generateMonthlySummary(eq(USER_ID), any());
        }

        @Test
        @DisplayName("Should override housing costs when housingMonthlyOverride is provided")
        void shouldOverrideHousingCosts() {
            setupDefaultMocks();

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null,
                    new BigDecimal("1500.00"), // monthly housing override
                    null, null, null);

            // 1500 × 12 = 18000
            assertEquals(0, new BigDecimal("18000.00").compareTo(projection.getHousingCosts()),
                    "Housing costs should be overridden to 1500/mo × 12 = 18000");
            verify(housingExpenseService, never()).calculateTotalAnnualHousingCosts(USER_ID);
        }

        @Test
        @DisplayName("Should override debt payments when debtMonthlyOverride is provided")
        void shouldOverrideDebtPayments() {
            setupDefaultMocks();

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null, null,
                    new BigDecimal("200.00"), // monthly debt override
                    null, null);

            // 200 × 12 = 2400
            assertEquals(0, new BigDecimal("2400.00").compareTo(projection.getDebtPayments()),
                    "Debt payments should be overridden to 200/mo × 12 = 2400");
            verify(debtService, never()).findAllByUserAsDto(any());
        }

        @Test
        @DisplayName("Should override subscription costs when subscriptionMonthlyOverride is provided")
        void shouldOverrideSubscriptionCosts() {
            setupDefaultMocks();

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null, null, null,
                    new BigDecimal("75.00"), // monthly subscription override
                    null);

            // 75 × 12 = 900
            assertEquals(0, new BigDecimal("900.00").compareTo(projection.getSubscriptionCosts()),
                    "Subscription costs should be overridden to 75/mo × 12 = 900");
            verify(subscriptionService, never()).calculateCurrentMonthTotal(USER_ID);
        }

        @Test
        @DisplayName("Should override holiday costs when holidayMonthlyOverride is provided")
        void shouldOverrideHolidayCosts() {
            setupDefaultMocks();

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null, null, null, null,
                    new BigDecimal("250.00")); // monthly holiday override

            // 250 × 12 = 3000
            assertEquals(0, new BigDecimal("3000.00").compareTo(projection.getHolidayCosts()),
                    "Holiday costs should be overridden to 250/mo × 12 = 3000");
            verify(holidayService, never()).findActiveByUserIdAsDto(USER_ID);
        }

        @Test
        @DisplayName("Should correctly compute totals when all overrides are provided")
        void shouldComputeTotalsWithAllOverrides() {
            setupDefaultMocks();

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

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("5000.00"),  // income: 60000/yr
                    new BigDecimal("1000.00"),  // housing: 12000/yr
                    new BigDecimal("200.00"),   // debt: 2400/yr
                    new BigDecimal("50.00"),    // subscriptions: 600/yr
                    new BigDecimal("100.00"));  // holidays: 1200/yr

            // Budget: 500 × 12 = 6000 (not overridden)
            // Total expenses: 6000 + 12000 + 2400 + 600 + 1200 = 22200
            assertEquals(0, new BigDecimal("60000.00").compareTo(projection.getProjectedIncome()));
            assertEquals(0, new BigDecimal("22200.00").compareTo(
                    projection.getProjectedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)));
            // Net savings: 60000 - 22200 = 37800
            assertEquals(0, new BigDecimal("37800.00").compareTo(
                    projection.getProjectedNetSavings().setScale(2, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should mix overridden and non-overridden values correctly")
        void shouldMixOverriddenAndNonOverriddenValues() {
            setupDefaultMocks();

            // Real housing = 12000
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));
            // Real subscriptions = 50/mo × 12 = 600
            when(subscriptionService.calculateCurrentMonthTotal(USER_ID))
                    .thenReturn(new BigDecimal("50.00"));

            // Override only debt and holidays, leave housing and subscriptions as real
            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null, // no income override — uses real (0 from mocks)
                    null, // no housing override — uses real (12000)
                    new BigDecimal("300.00"), // debt override: 3600/yr
                    null, // no subscription override — uses real (600)
                    new BigDecimal("200.00")); // holiday override: 2400/yr

            assertEquals(0, new BigDecimal("12000.00").compareTo(projection.getHousingCosts()),
                    "Housing should use real value");
            assertEquals(0, new BigDecimal("600.00").compareTo(projection.getSubscriptionCosts()),
                    "Subscriptions should use real value");
            assertEquals(0, new BigDecimal("3600.00").compareTo(projection.getDebtPayments()),
                    "Debt should use overridden value");
            assertEquals(0, new BigDecimal("2400.00").compareTo(projection.getHolidayCosts()),
                    "Holidays should use overridden value");
        }

        @Test
        @DisplayName("Should handle zero override values correctly")
        void shouldHandleZeroOverrides() {
            setupDefaultMocks();
            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    null,
                    BigDecimal.ZERO, // housing override to zero
                    null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getHousingCosts()),
                    "Housing should be zero when overridden to zero");
        }
    }

    @Nested
    @DisplayName("Budget Override Tests")
    class BudgetOverrideTests {

        @Test
        @DisplayName("Should replace overridden category with synthetic MONTHLY budget")
        void shouldReplaceOverriddenCategoryBudget() {
            setupDefaultMocks();

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

            // Override Groceries to 600/mo, leave Entertainment unchanged
            Map<Long, BigDecimal> overrides = Map.of(1L, new BigDecimal("600.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            // Groceries: 600 × 12 = 7200, Entertainment: 200 × 12 = 2400
            // Total budgeted: 7200 + 2400 = 9600
            assertEquals(0, new BigDecimal("9600.00").compareTo(
                    projection.getBudgetedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Budgeted expenses should reflect overridden Groceries amount");
        }

        @Test
        @DisplayName("Should consolidate multiple budgets for same category into one synthetic budget")
        void shouldConsolidateMultipleBudgetsForOverriddenCategory() {
            setupDefaultMocks();

            // Two Groceries budgets for different halves of the year
            when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            BudgetDTO.builder()
                                    .categoryId(1L)
                                    .categoryName("Groceries")
                                    .categoryColorCode("#FF0000")
                                    .amount(new BigDecimal("400.00"))
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
                    ));

            // Without override: 400×6 + 600×6 = 6000
            AnnualProjectionDTO base = annualProjectionService.generateProjection(USER_ID, TEST_YEAR);
            assertEquals(0, new BigDecimal("6000.00").compareTo(base.getBudgetedExpenses()));

            // Override to 700/mo — both original budgets replaced by one synthetic: 700×12 = 8400
            Map<Long, BigDecimal> overrides = Map.of(1L, new BigDecimal("700.00"));
            AnnualProjectionDTO overridden = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            assertEquals(0, new BigDecimal("8400.00").compareTo(
                    overridden.getBudgetedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Both original budgets should be replaced by one synthetic 700/mo budget");
        }

        @Test
        @DisplayName("Should leave non-overridden categories unchanged")
        void shouldLeaveNonOverriddenCategoriesUnchanged() {
            setupDefaultMocks();

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

            // Override only Groceries
            Map<Long, BigDecimal> overrides = Map.of(1L, new BigDecimal("800.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            // Find Entertainment in category projections — should be 200 × 12 = 2400
            CategoryProjection entertainment = projection.getCategoryProjections().stream()
                    .filter(c -> "Entertainment".equals(c.getCategoryName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(entertainment, "Entertainment category should still be present");
            assertEquals(0, new BigDecimal("2400.00").compareTo(entertainment.getAnnualAmount()),
                    "Entertainment should be unchanged at 200 × 12 = 2400");
        }

        @Test
        @DisplayName("Should override budget to zero effectively removing the category")
        void shouldOverrideBudgetToZero() {
            setupDefaultMocks();

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

            // Override to zero
            Map<Long, BigDecimal> overrides = Map.of(1L, BigDecimal.ZERO);

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            assertEquals(0, BigDecimal.ZERO.compareTo(projection.getBudgetedExpenses()),
                    "Budgeted expenses should be zero when only category is overridden to zero");
        }

        @Test
        @DisplayName("Should override WEEKLY budget category correctly")
        void shouldOverrideWeeklyBudgetCategory() {
            setupDefaultMocks();

            when(budgetService.findByUserIdAndDateRange(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            BudgetDTO.builder()
                                    .categoryId(1L)
                                    .categoryName("Groceries")
                                    .categoryColorCode("#FF0000")
                                    .amount(new BigDecimal("100.00"))
                                    .periodType(PeriodType.WEEKLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build()
                    ));

            // Override to 450/mo (synthetic is MONTHLY) → 450 × 12 = 5400
            Map<Long, BigDecimal> overrides = Map.of(1L, new BigDecimal("450.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            assertEquals(0, new BigDecimal("5400.00").compareTo(
                    projection.getBudgetedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Overridden weekly budget should be replaced with 450/mo × 12 = 5400");
        }

        @Test
        @DisplayName("Should handle multiple category overrides simultaneously")
        void shouldHandleMultipleCategoryOverrides() {
            setupDefaultMocks();

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
                                    .build(),
                            BudgetDTO.builder()
                                    .categoryId(2L)
                                    .categoryName("Entertainment")
                                    .categoryColorCode("#00FF00")
                                    .amount(new BigDecimal("200.00"))
                                    .periodType(PeriodType.MONTHLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build(),
                            BudgetDTO.builder()
                                    .categoryId(3L)
                                    .categoryName("Transport")
                                    .categoryColorCode("#0000FF")
                                    .amount(new BigDecimal("150.00"))
                                    .periodType(PeriodType.MONTHLY)
                                    .startDate(LocalDate.of(TEST_YEAR, 1, 1))
                                    .endDate(LocalDate.of(TEST_YEAR, 12, 31))
                                    .build()
                    ));

            // Override Groceries and Transport, leave Entertainment
            Map<Long, BigDecimal> overrides = Map.of(
                    1L, new BigDecimal("600.00"),  // Groceries: 600×12 = 7200
                    3L, new BigDecimal("100.00")   // Transport: 100×12 = 1200
            );

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            // Entertainment unchanged: 200×12 = 2400
            // Total: 7200 + 2400 + 1200 = 10800
            assertEquals(0, new BigDecimal("10800.00").compareTo(
                    projection.getBudgetedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Total budgeted expenses should reflect both overrides");
        }

        @Test
        @DisplayName("Should handle override for category not present in budgets (no effect)")
        void shouldHandleOverrideForAbsentCategory() {
            setupDefaultMocks();

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

            // Override for category 99 which doesn't exist in budgets
            Map<Long, BigDecimal> overrides = Map.of(99L, new BigDecimal("300.00"));

            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, overrides, null, null, null, null, null);

            // Groceries should be unchanged: 500 × 12 = 6000
            // The nonexistent category override creates no synthetic budget (no matching original)
            assertEquals(0, new BigDecimal("6000.00").compareTo(
                    projection.getBudgetedExpenses().setScale(2, java.math.RoundingMode.HALF_UP)),
                    "Override for absent category should have no effect");
        }
    }

    @Nested
    @DisplayName("Net Savings and Savings Rate with Overrides Tests")
    class SavingsWithOverridesTests {

        @Test
        @DisplayName("Should recalculate net savings when income is overridden higher")
        void shouldRecalculateNetSavingsWithHigherIncome() {
            setupDefaultMocks();

            when(housingExpenseService.calculateTotalAnnualHousingCosts(USER_ID))
                    .thenReturn(new BigDecimal("12000.00"));

            // Override income to 6000/mo = 72000/yr, expenses = 12000 (housing only)
            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("6000.00"), null, null, null, null);

            // Net savings: 72000 - 12000 = 60000
            assertEquals(0, new BigDecimal("60000.00").compareTo(
                    projection.getProjectedNetSavings().setScale(2, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should produce negative savings when expenses exceed income")
        void shouldProduceNegativeSavingsWhenExpensesExceedIncome() {
            setupDefaultMocks();

            // Low income, high overrides
            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("1000.00"),   // income: 12000/yr
                    new BigDecimal("2000.00"),   // housing: 24000/yr
                    null, null, null);

            // Net savings: 12000 - 24000 = -12000
            assertTrue(projection.getProjectedNetSavings().compareTo(BigDecimal.ZERO) < 0,
                    "Net savings should be negative when expenses exceed income");
            assertEquals(0, new BigDecimal("-12000.00").compareTo(
                    projection.getProjectedNetSavings().setScale(2, java.math.RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("Should correctly calculate savings rate with overrides")
        void shouldCalculateSavingsRateWithOverrides() {
            setupDefaultMocks();

            // Income: 5000/mo = 60000/yr, Housing: 1000/mo = 12000/yr
            // Net savings: 48000, Rate: 48000/60000 × 100 = 80%
            AnnualProjectionDTO projection = annualProjectionService.generateProjectionWithOverrides(
                    USER_ID, TEST_YEAR, Map.of(),
                    new BigDecimal("5000.00"),
                    new BigDecimal("1000.00"),
                    null, null, null);

            assertEquals(0, new BigDecimal("80.00").compareTo(projection.getProjectedSavingsRate()),
                    "Savings rate should be 80% when saving 48000 of 60000");
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
