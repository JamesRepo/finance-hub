package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtPayoffPlan;
import com.jameselner.finance_hub.dto.DebtPayoffProjection;
import com.jameselner.finance_hub.repository.DebtRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebtPayoffCalculatorService Tests")
class DebtPayoffCalculatorServiceTest {

    @Mock
    private DebtService debtService;

    @Mock
    private DebtRepository debtRepository;

    @InjectMocks
    private DebtPayoffCalculatorService calculatorService;

    private User testUser;
    private Debt creditCardDebt;
    private Debt studentLoanDebt;
    private Debt carLoanDebt;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);

        creditCardDebt = new Debt();
        creditCardDebt.setDebtId(1L);
        creditCardDebt.setUser(testUser);
        creditCardDebt.setDebtName("Credit Card");
        creditCardDebt.setDebtType(DebtType.CREDIT_CARD);
        creditCardDebt.setPrincipalAmount(new BigDecimal("5000.00"));
        creditCardDebt.setCurrentBalance(new BigDecimal("3000.00"));
        creditCardDebt.setInterestRate(new BigDecimal("24.00"));
        creditCardDebt.setStartDate(LocalDate.of(2023, 1, 1));
        creditCardDebt.setMinimumPayment(new BigDecimal("100.00"));

        studentLoanDebt = new Debt();
        studentLoanDebt.setDebtId(2L);
        studentLoanDebt.setUser(testUser);
        studentLoanDebt.setDebtName("Student Loan");
        studentLoanDebt.setDebtType(DebtType.STUDENT_LOAN);
        studentLoanDebt.setPrincipalAmount(new BigDecimal("20000.00"));
        studentLoanDebt.setCurrentBalance(new BigDecimal("15000.00"));
        studentLoanDebt.setInterestRate(new BigDecimal("6.00"));
        studentLoanDebt.setStartDate(LocalDate.of(2020, 1, 1));
        studentLoanDebt.setMinimumPayment(new BigDecimal("200.00"));

        carLoanDebt = new Debt();
        carLoanDebt.setDebtId(3L);
        carLoanDebt.setUser(testUser);
        carLoanDebt.setDebtName("Car Loan");
        carLoanDebt.setDebtType(DebtType.AUTO_LOAN);
        carLoanDebt.setPrincipalAmount(new BigDecimal("15000.00"));
        carLoanDebt.setCurrentBalance(new BigDecimal("10000.00"));
        carLoanDebt.setInterestRate(new BigDecimal("4.50"));
        carLoanDebt.setStartDate(LocalDate.of(2022, 1, 1));
        carLoanDebt.setMinimumPayment(new BigDecimal("150.00"));
    }

    @Nested
    @DisplayName("calculatePayoffProjection Tests")
    class CalculatePayoffProjectionTests {

        @Test
        @DisplayName("Should calculate payoff projection for debt")
        void shouldCalculatePayoffProjection() {
            BigDecimal monthlyPayment = new BigDecimal("500.00");

            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), eq(creditCardDebt.getInterestRate())))
                    .thenAnswer(invocation -> {
                        BigDecimal balance = invocation.getArgument(0);
                        BigDecimal rate = invocation.getArgument(1);
                        return balance.multiply(rate.divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP))
                                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    });

            DebtPayoffProjection projection = calculatorService.calculatePayoffProjection(
                    creditCardDebt, monthlyPayment
            );

            assertNotNull(projection);
            assertEquals(creditCardDebt.getDebtId(), projection.getDebtId());
            assertEquals(creditCardDebt.getDebtName(), projection.getDebtName());
            assertEquals(creditCardDebt.getCurrentBalance(), projection.getCurrentBalance());
            assertEquals(monthlyPayment, projection.getMonthlyPayment());
            assertTrue(projection.getMonthsToPayoff() > 0);
            assertNotNull(projection.getProjectedPayoffDate());
            assertTrue(projection.getTotalInterestPaid().compareTo(BigDecimal.ZERO) >= 0);
        }

        @Test
        @DisplayName("Should handle zero balance debt")
        void shouldHandleZeroBalanceDebt() {
            creditCardDebt.setCurrentBalance(BigDecimal.ZERO);
            BigDecimal monthlyPayment = new BigDecimal("500.00");

            DebtPayoffProjection projection = calculatorService.calculatePayoffProjection(
                    creditCardDebt, monthlyPayment
            );

            assertNotNull(projection);
            assertEquals(0, projection.getMonthsToPayoff());
            assertEquals(BigDecimal.ZERO, projection.getTotalInterestPaid());
            assertEquals(LocalDate.now(), projection.getProjectedPayoffDate());
        }

        @Test
        @DisplayName("Should throw exception when debt is null")
        void shouldThrowExceptionWhenDebtIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffProjection(null, new BigDecimal("500.00"))
            );
            assertEquals("Debt must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is null")
        void shouldThrowExceptionWhenMonthlyPaymentIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffProjection(creditCardDebt, null)
            );
            assertEquals("Monthly payment must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is zero")
        void shouldThrowExceptionWhenMonthlyPaymentIsZero() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffProjection(creditCardDebt, BigDecimal.ZERO)
            );
            assertEquals("Monthly payment must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is negative")
        void shouldThrowExceptionWhenMonthlyPaymentIsNegative() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffProjection(creditCardDebt, new BigDecimal("-100.00"))
            );
            assertEquals("Monthly payment must be greater than zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("calculateAvalancheMethod Tests")
    class CalculateAvalancheMethodTests {

        @Test
        @DisplayName("Should calculate avalanche method correctly")
        void shouldCalculateAvalancheMethod() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("1000.00");

            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenAnswer(invocation -> {
                        BigDecimal balance = invocation.getArgument(0);
                        BigDecimal rate = invocation.getArgument(1);
                        return balance.multiply(rate.divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP))
                                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    });

            DebtPayoffPlan plan = calculatorService.calculateAvalancheMethod(debts, totalMonthlyPayment);

            assertNotNull(plan);
            assertEquals("Avalanche (Highest Interest First)", plan.getStrategyName());
            assertEquals(totalMonthlyPayment, plan.getTotalMonthlyPayment());
            assertEquals(3, plan.getDebtProjections().size());

            DebtPayoffProjection firstDebt = plan.getDebtProjections().get(0);
            assertEquals(creditCardDebt.getDebtId(), firstDebt.getDebtId());
        }

        @Test
        @DisplayName("Should prioritize highest interest rate debt")
        void shouldPrioritizeHighestInterestRateDebt() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("1000.00");

            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            DebtPayoffPlan plan = calculatorService.calculateAvalancheMethod(debts, totalMonthlyPayment);

            DebtPayoffProjection firstProjection = plan.getDebtProjections().get(0);
            assertEquals("Credit Card", firstProjection.getDebtName());

            BigDecimal minimumPaymentsSum = new BigDecimal("450.00");
            BigDecimal extraPayment = totalMonthlyPayment.subtract(minimumPaymentsSum);
            BigDecimal expectedFirstPayment = creditCardDebt.getMinimumPayment().add(extraPayment);

            assertEquals(expectedFirstPayment, firstProjection.getMonthlyPayment());
        }

        @Test
        @DisplayName("Should throw exception when debts list is null")
        void shouldThrowExceptionWhenDebtsListIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAvalancheMethod(null, new BigDecimal("1000.00"))
            );
            assertEquals("Debts list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debts list is empty")
        void shouldThrowExceptionWhenDebtsListIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAvalancheMethod(Collections.emptyList(), new BigDecimal("1000.00"))
            );
            assertEquals("Debts list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when total payment is less than minimum payments")
        void shouldThrowExceptionWhenTotalPaymentTooLow() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("100.00");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateAvalancheMethod(debts, totalMonthlyPayment)
            );
            assertTrue(exception.getMessage().contains("must be at least the sum of all minimum payments"));
        }
    }

    @Nested
    @DisplayName("calculateSnowballMethod Tests")
    class CalculateSnowballMethodTests {

        @Test
        @DisplayName("Should calculate snowball method correctly")
        void shouldCalculateSnowballMethod() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("1000.00");

            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenAnswer(invocation -> {
                        BigDecimal balance = invocation.getArgument(0);
                        BigDecimal rate = invocation.getArgument(1);
                        return balance.multiply(rate.divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP))
                                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    });

            DebtPayoffPlan plan = calculatorService.calculateSnowballMethod(debts, totalMonthlyPayment);

            assertNotNull(plan);
            assertEquals("Snowball (Lowest Balance First)", plan.getStrategyName());
            assertEquals(totalMonthlyPayment, plan.getTotalMonthlyPayment());
            assertEquals(3, plan.getDebtProjections().size());

            DebtPayoffProjection firstDebt = plan.getDebtProjections().get(0);
            assertEquals(creditCardDebt.getDebtId(), firstDebt.getDebtId());
        }

        @Test
        @DisplayName("Should prioritize lowest balance debt")
        void shouldPrioritizeLowestBalanceDebt() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("1000.00");

            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            DebtPayoffPlan plan = calculatorService.calculateSnowballMethod(debts, totalMonthlyPayment);

            DebtPayoffProjection firstProjection = plan.getDebtProjections().get(0);
            assertEquals("Credit Card", firstProjection.getDebtName());

            BigDecimal minimumPaymentsSum = new BigDecimal("450.00");
            BigDecimal extraPayment = totalMonthlyPayment.subtract(minimumPaymentsSum);
            BigDecimal expectedFirstPayment = creditCardDebt.getMinimumPayment().add(extraPayment);

            assertEquals(expectedFirstPayment, firstProjection.getMonthlyPayment());
        }

        @Test
        @DisplayName("Should throw exception when debts list is null")
        void shouldThrowExceptionWhenDebtsListIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateSnowballMethod(null, new BigDecimal("1000.00"))
            );
            assertEquals("Debts list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debts list is empty")
        void shouldThrowExceptionWhenDebtsListIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateSnowballMethod(Collections.emptyList(), new BigDecimal("1000.00"))
            );
            assertEquals("Debts list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when total payment is less than minimum payments")
        void shouldThrowExceptionWhenTotalPaymentTooLow() {
            List<Debt> debts = Arrays.asList(creditCardDebt, studentLoanDebt, carLoanDebt);
            BigDecimal totalMonthlyPayment = new BigDecimal("100.00");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateSnowballMethod(debts, totalMonthlyPayment)
            );
            assertTrue(exception.getMessage().contains("must be at least the sum of all minimum payments"));
        }
    }

    @Nested
    @DisplayName("calculateMonthsToPayoff Tests")
    class CalculateMonthsToPayoffTests {

        @Test
        @DisplayName("Should calculate months to payoff correctly")
        void shouldCalculateMonthsToPayoff() {
            BigDecimal currentBalance = new BigDecimal("1000.00");
            BigDecimal interestRate = new BigDecimal("12.00");
            BigDecimal monthlyPayment = new BigDecimal("100.00");

            int months = calculatorService.calculateMonthsToPayoff(
                    currentBalance, interestRate, monthlyPayment
            );

            assertTrue(months > 0);
            assertTrue(months < 1200);
        }

        @Test
        @DisplayName("Should return zero for zero balance")
        void shouldReturnZeroForZeroBalance() {
            int months = calculatorService.calculateMonthsToPayoff(
                    BigDecimal.ZERO, new BigDecimal("12.00"), new BigDecimal("100.00")
            );

            assertEquals(0, months);
        }

        @Test
        @DisplayName("Should throw exception when current balance is null")
        void shouldThrowExceptionWhenCurrentBalanceIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateMonthsToPayoff(
                            null, new BigDecimal("12.00"), new BigDecimal("100.00")
                    )
            );
            assertEquals("All parameters must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when interest rate is null")
        void shouldThrowExceptionWhenInterestRateIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateMonthsToPayoff(
                            new BigDecimal("1000.00"), null, new BigDecimal("100.00")
                    )
            );
            assertEquals("All parameters must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is null")
        void shouldThrowExceptionWhenMonthlyPaymentIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateMonthsToPayoff(
                            new BigDecimal("1000.00"), new BigDecimal("12.00"), null
                    )
            );
            assertEquals("All parameters must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is zero")
        void shouldThrowExceptionWhenMonthlyPaymentIsZero() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateMonthsToPayoff(
                            new BigDecimal("1000.00"), new BigDecimal("12.00"), BigDecimal.ZERO
                    )
            );
            assertEquals("Monthly payment must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is negative")
        void shouldThrowExceptionWhenMonthlyPaymentIsNegative() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculateMonthsToPayoff(
                            new BigDecimal("1000.00"), new BigDecimal("12.00"), new BigDecimal("-50.00")
                    )
            );
            assertEquals("Monthly payment must be greater than zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("calculatePayoffPlans Tests")
    class CalculatePayoffPlansTests {

        @Test
        @DisplayName("Should return avalanche and snowball plans")
        void shouldReturnBothPlans() {
            when(debtRepository.findById(1L)).thenReturn(Optional.of(creditCardDebt));
            when(debtRepository.findById(2L)).thenReturn(Optional.of(studentLoanDebt));
            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenAnswer(invocation -> {
                        BigDecimal balance = invocation.getArgument(0);
                        BigDecimal rate = invocation.getArgument(1);
                        return balance.multiply(rate.divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP))
                                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    });

            List<DebtPayoffPlan> plans = calculatorService.calculatePayoffPlans(
                    Arrays.asList(1L, 2L), new BigDecimal("1000.00"));

            assertEquals(2, plans.size());
            assertEquals("Avalanche (Highest Interest First)", plans.get(0).getStrategyName());
            assertEquals("Snowball (Lowest Balance First)", plans.get(1).getStrategyName());
        }

        @Test
        @DisplayName("Should not mutate original entity balances")
        void shouldNotMutateOriginalEntityBalances() {
            BigDecimal originalCcBalance = creditCardDebt.getCurrentBalance();
            BigDecimal originalSlBalance = studentLoanDebt.getCurrentBalance();

            when(debtRepository.findById(1L)).thenReturn(Optional.of(creditCardDebt));
            when(debtRepository.findById(2L)).thenReturn(Optional.of(studentLoanDebt));
            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            calculatorService.calculatePayoffPlans(
                    Arrays.asList(1L, 2L), new BigDecimal("1000.00"));

            assertEquals(originalCcBalance, creditCardDebt.getCurrentBalance(),
                    "Credit card balance should not be mutated");
            assertEquals(originalSlBalance, studentLoanDebt.getCurrentBalance(),
                    "Student loan balance should not be mutated");
        }

        @Test
        @DisplayName("Should filter out deleted debts")
        void shouldFilterOutDeletedDebts() {
            creditCardDebt.setDeleted(true);

            when(debtRepository.findById(1L)).thenReturn(Optional.of(creditCardDebt));
            when(debtRepository.findById(2L)).thenReturn(Optional.of(studentLoanDebt));
            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            List<DebtPayoffPlan> plans = calculatorService.calculatePayoffPlans(
                    Arrays.asList(1L, 2L), new BigDecimal("1000.00"));

            assertEquals(2, plans.size());
            assertEquals(1, plans.get(0).getDebtProjections().size());
            assertEquals("Student Loan", plans.get(0).getDebtProjections().get(0).getDebtName());
        }

        @Test
        @DisplayName("Should skip non-existent debt IDs")
        void shouldSkipNonExistentDebtIds() {
            when(debtRepository.findById(1L)).thenReturn(Optional.of(creditCardDebt));
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());
            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            List<DebtPayoffPlan> plans = calculatorService.calculatePayoffPlans(
                    Arrays.asList(1L, 999L), new BigDecimal("1000.00"));

            assertEquals(2, plans.size());
            assertEquals(1, plans.get(0).getDebtProjections().size());
        }

        @Test
        @DisplayName("Should throw exception when all debt IDs are invalid")
        void shouldThrowExceptionWhenAllDebtIdsInvalid() {
            when(debtRepository.findById(998L)).thenReturn(Optional.empty());
            when(debtRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffPlans(
                            Arrays.asList(998L, 999L), new BigDecimal("1000.00"))
            );
            assertEquals("No valid debts found for the given IDs", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt IDs list is null")
        void shouldThrowExceptionWhenDebtIdsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffPlans(null, new BigDecimal("1000.00"))
            );
            assertEquals("Debt IDs list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debt IDs list is empty")
        void shouldThrowExceptionWhenDebtIdsEmpty() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffPlans(
                            Collections.emptyList(), new BigDecimal("1000.00"))
            );
            assertEquals("Debt IDs list must not be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when monthly payment is null")
        void shouldThrowExceptionWhenPaymentNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> calculatorService.calculatePayoffPlans(Arrays.asList(1L, 2L), null)
            );
            assertEquals("Total monthly payment must be greater than zero", exception.getMessage());
        }

        @Test
        @DisplayName("Should default null minimum payment to zero in working copy")
        void shouldDefaultNullMinimumPaymentToZero() {
            creditCardDebt.setMinimumPayment(null);

            when(debtRepository.findById(1L)).thenReturn(Optional.of(creditCardDebt));
            when(debtService.calculateMonthlyInterest(any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            List<DebtPayoffPlan> plans = calculatorService.calculatePayoffPlans(
                    List.of(1L), new BigDecimal("500.00"));

            assertNotNull(plans);
            assertEquals(2, plans.size());
        }
    }
}
