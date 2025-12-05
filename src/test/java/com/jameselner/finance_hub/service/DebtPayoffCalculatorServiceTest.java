package com.jameselner.finance_hub.service;

import com.jameselner.finance_hub.domain.Debt;
import com.jameselner.finance_hub.domain.User;
import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtPayoffPlan;
import com.jameselner.finance_hub.dto.DebtPayoffProjection;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebtPayoffCalculatorService Tests")
class DebtPayoffCalculatorServiceTest {

    @Mock
    private DebtService debtService;

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
}
