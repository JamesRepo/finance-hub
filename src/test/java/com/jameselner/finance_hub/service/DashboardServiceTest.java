package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.IncomeSource;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.TransactionType;
import com.jameselner.finance_hub.dto.DashboardSummaryDto;
import com.jameselner.finance_hub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private IncomeSourceRepository incomeSourceRepository;

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
    }

    @Nested
    @DisplayName("getDashboardSummary")
    class GetDashboardSummary {

        @Test
        @DisplayName("returns complete summary with all values populated")
        void returnsCompleteSummaryWithAllValues() {
            IncomeSource monthlyIncome = createIncomeSource(new BigDecimal("3500.00"));

            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(new BigDecimal("25000.00"));
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(monthlyIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("2100.00"));
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(new BigDecimal("15000.00"));
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(new BigDecimal("8000.00"));

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertNotNull(result);
            assertEquals(0, new BigDecimal("25000.00").compareTo(result.totalBalance()), "totalBalance");
            assertEquals(0, new BigDecimal("3500.00").compareTo(result.lastMonthlyIncome()), "lastMonthlyIncome");
            assertEquals(0, new BigDecimal("2100.00").compareTo(result.monthlyExpenses()), "monthlyExpenses");
            assertEquals(0, new BigDecimal("15000.00").compareTo(result.totalDebt()), "totalDebt");
            assertEquals(0, new BigDecimal("8000.00").compareTo(result.totalSavings()), "totalSavings");
        }

        @Test
        @DisplayName("returns zero values when user has no financial data")
        void returnsZeroValuesWhenNoData() {
            IncomeSource zeroIncome = createIncomeSource(BigDecimal.ZERO);

            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(zeroIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.totalBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.lastMonthlyIncome()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.monthlyExpenses()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.totalDebt()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.totalSavings()));
        }

        @Test
        @DisplayName("queries expenses for current month date range only")
        void queriesExpensesForCurrentMonthOnly() {
            YearMonth currentMonth = YearMonth.now();
            LocalDate expectedStart = currentMonth.atDay(1);
            LocalDate expectedEnd = currentMonth.atEndOfMonth();

            IncomeSource monthlyIncome = createIncomeSource(new BigDecimal("3000.00"));
            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(monthlyIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), eq(expectedStart), eq(expectedEnd)))
                    .thenReturn(new BigDecimal("500.00"));
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            dashboardService.getDashboardSummary(testUser);

            verify(transactionRepository).sumByUserAndTypeAndDateRange(
                    testUser, TransactionType.EXPENSE, expectedStart, expectedEnd);
        }

        @Test
        @DisplayName("only queries EXPENSE transaction type, not INCOME")
        void onlyQueriesExpenseTransactionType() {
            IncomeSource monthlyIncome = createIncomeSource(new BigDecimal("3000.00"));
            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(monthlyIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            dashboardService.getDashboardSummary(testUser);

            verify(transactionRepository, never()).sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("returns zero income when no monthly income source exists")
        void returnsZeroIncomeWhenNoMonthlyIncomeSourceExists() {
            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(new BigDecimal("10000.00"));
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(null);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("500.00"));
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.lastMonthlyIncome()),
                    "Should default to zero when no monthly income source exists");
            assertEquals(0, new BigDecimal("10000.00").compareTo(result.totalBalance()),
                    "Other fields should still be populated");
        }

        @Test
        @DisplayName("returns zero income when income source has null net amount")
        void returnsZeroIncomeWhenNetAmountIsNull() {
            IncomeSource incomeSource = new IncomeSource();
            incomeSource.setGrossAmount(new BigDecimal("5000.00"));
            incomeSource.setNetAmount(null);

            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(incomeSource);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.lastMonthlyIncome()),
                    "Should default to zero when netAmount is null");
        }

        @Test
        @DisplayName("uses net amount from income source, not gross")
        void usesNetAmountFromIncomeSource() {
            IncomeSource incomeSource = new IncomeSource();
            incomeSource.setGrossAmount(new BigDecimal("5000.00"));
            incomeSource.setNetAmount(new BigDecimal("3800.00"));

            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(incomeSource);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertEquals(0, new BigDecimal("3800.00").compareTo(result.lastMonthlyIncome()),
                    "Should use netAmount, not grossAmount");
        }

        @Test
        @DisplayName("calls all five repositories exactly once")
        void callsAllRepositoriesOnce() {
            IncomeSource monthlyIncome = createIncomeSource(new BigDecimal("3000.00"));
            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(monthlyIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(BigDecimal.ZERO);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(BigDecimal.ZERO);

            dashboardService.getDashboardSummary(testUser);

            verify(accountRepository, times(1)).getTotalBalanceByUser(testUser);
            verify(incomeSourceRepository, times(1)).getLastMonthlyIncomeByUser(testUser);
            verify(transactionRepository, times(1)).sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class));
            verify(debtRepository, times(1)).getTotalDebtByUser(testUser);
            verify(savingsGoalRepository, times(1)).getTotalCurrentSavingsAmountByUser(testUser);
        }

        @Test
        @DisplayName("handles large financial values correctly")
        void handlesLargeValues() {
            BigDecimal largeBalance = new BigDecimal("9999999999.99");
            BigDecimal largeIncome = new BigDecimal("999999.99");
            BigDecimal largeExpenses = new BigDecimal("888888.88");
            BigDecimal largeDebt = new BigDecimal("5555555.55");
            BigDecimal largeSavings = new BigDecimal("3333333.33");

            IncomeSource monthlyIncome = createIncomeSource(largeIncome);
            when(accountRepository.getTotalBalanceByUser(testUser)).thenReturn(largeBalance);
            when(incomeSourceRepository.getLastMonthlyIncomeByUser(testUser)).thenReturn(monthlyIncome);
            when(transactionRepository.sumByUserAndTypeAndDateRange(
                    eq(testUser), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(largeExpenses);
            when(debtRepository.getTotalDebtByUser(testUser)).thenReturn(largeDebt);
            when(savingsGoalRepository.getTotalCurrentSavingsAmountByUser(testUser)).thenReturn(largeSavings);

            DashboardSummaryDto result = dashboardService.getDashboardSummary(testUser);

            assertEquals(0, largeBalance.compareTo(result.totalBalance()));
            assertEquals(0, largeIncome.compareTo(result.lastMonthlyIncome()));
            assertEquals(0, largeExpenses.compareTo(result.monthlyExpenses()));
            assertEquals(0, largeDebt.compareTo(result.totalDebt()));
            assertEquals(0, largeSavings.compareTo(result.totalSavings()));
        }
    }

    private IncomeSource createIncomeSource(BigDecimal netAmount) {
        IncomeSource incomeSource = new IncomeSource();
        incomeSource.setNetAmount(netAmount);
        incomeSource.setGrossAmount(netAmount);
        return incomeSource;
    }
}
