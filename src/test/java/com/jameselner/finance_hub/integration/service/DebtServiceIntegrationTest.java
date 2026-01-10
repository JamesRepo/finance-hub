package com.jameselner.finance_hub.integration.service;

import com.jameselner.finance_hub.domain.enums.DebtType;
import com.jameselner.finance_hub.dto.DebtDTO;
import com.jameselner.finance_hub.integration.TransactionalIntegrationTest;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory;
import com.jameselner.finance_hub.integration.fixtures.TestFixtureFactory.TestUserContext;
import com.jameselner.finance_hub.repository.DebtRepository;
import com.jameselner.finance_hub.service.DebtService;
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

@DisplayName("DebtService Integration Tests")
class DebtServiceIntegrationTest extends TransactionalIntegrationTest {

    @Autowired
    private DebtService debtService;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private TestFixtureFactory fixtureFactory;

    private TestUserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = fixtureFactory.createBasicUser("debt-test@example.com");
    }

    @Nested
    @DisplayName("Debt CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should create debt")
        void shouldCreateDebt() {
            DebtDTO dto = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Credit Card")
                    .debtType(DebtType.CREDIT_CARD)
                    .principalAmount(new BigDecimal("5000.00"))
                    .currentBalance(new BigDecimal("4500.00"))
                    .interestRate(new BigDecimal("19.99"))
                    .startDate(LocalDate.now().minusMonths(6))
                    .minimumPayment(new BigDecimal("100.00"))
                    .build();

            DebtDTO created = debtService.createDebtFromDto(dto);

            assertNotNull(created.getDebtId());
            assertEquals("Credit Card", created.getDebtName());
            assertEquals(DebtType.CREDIT_CARD, created.getDebtType());
            assertEquals(new BigDecimal("19.99"), created.getInterestRate());
        }

        @Test
        @DisplayName("Should create different debt types")
        void shouldCreateDifferentDebtTypes() {
            DebtDTO mortgage = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Home Mortgage")
                    .debtType(DebtType.MORTGAGE)
                    .principalAmount(new BigDecimal("250000.00"))
                    .currentBalance(new BigDecimal("240000.00"))
                    .interestRate(new BigDecimal("4.25"))
                    .startDate(LocalDate.now().minusYears(2))
                    .minimumPayment(new BigDecimal("1500.00"))
                    .build();
            DebtDTO createdMortgage = debtService.createDebtFromDto(mortgage);

            DebtDTO autoLoan = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Car Loan")
                    .debtType(DebtType.AUTO_LOAN)
                    .principalAmount(new BigDecimal("25000.00"))
                    .currentBalance(new BigDecimal("20000.00"))
                    .interestRate(new BigDecimal("6.99"))
                    .startDate(LocalDate.now().minusYears(1))
                    .minimumPayment(new BigDecimal("450.00"))
                    .build();
            DebtDTO createdAutoLoan = debtService.createDebtFromDto(autoLoan);

            assertEquals(DebtType.MORTGAGE, createdMortgage.getDebtType());
            assertEquals(DebtType.AUTO_LOAN, createdAutoLoan.getDebtType());
        }

        @Test
        @DisplayName("Should update debt")
        void shouldUpdateDebt() {
            DebtDTO dto = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Original Name")
                    .debtType(DebtType.PERSONAL_LOAN)
                    .principalAmount(new BigDecimal("10000.00"))
                    .currentBalance(new BigDecimal("8000.00"))
                    .interestRate(new BigDecimal("12.00"))
                    .startDate(LocalDate.now().minusMonths(3))
                    .minimumPayment(new BigDecimal("200.00"))
                    .build();
            DebtDTO created = debtService.createDebtFromDto(dto);

            created.setDebtName("Updated Name");
            created.setCurrentBalance(new BigDecimal("7500.00"));
            DebtDTO updated = debtService.updateDebtFromDto(created);

            assertEquals("Updated Name", updated.getDebtName());
            assertEquals(new BigDecimal("7500.00"), updated.getCurrentBalance());
        }

        @Test
        @DisplayName("Should delete debt")
        void shouldDeleteDebt() {
            DebtDTO dto = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("To Delete")
                    .debtType(DebtType.OTHER)
                    .principalAmount(new BigDecimal("1000.00"))
                    .currentBalance(new BigDecimal("500.00"))
                    .interestRate(new BigDecimal("5.00"))
                    .startDate(LocalDate.now())
                    .minimumPayment(new BigDecimal("50.00"))
                    .build();
            DebtDTO created = debtService.createDebtFromDto(dto);

            debtService.deleteById(created.getDebtId());

            Optional<DebtDTO> found = debtService.findByIdAsDto(created.getDebtId());
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Debt Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find all debts for user")
        void shouldFindAllDebtsForUser() {
            for (int i = 0; i < 3; i++) {
                DebtDTO dto = DebtDTO.builder()
                        .userId(userContext.user().getUserId())
                        .debtName("Debt " + i)
                        .debtType(DebtType.CREDIT_CARD)
                        .principalAmount(new BigDecimal("1000.00"))
                        .currentBalance(new BigDecimal("500.00"))
                        .interestRate(new BigDecimal("15.00").add(new BigDecimal(i)))
                        .startDate(LocalDate.now())
                        .minimumPayment(new BigDecimal("50.00"))
                        .build();
                debtService.createDebtFromDto(dto);
            }

            List<DebtDTO> debts = debtService.findAllAsDto();

            assertEquals(3, debts.size());
        }

        @Test
        @DisplayName("Should calculate total debt for user")
        void shouldCalculateTotalDebtForUser() {
            DebtDTO debt1 = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Debt 1")
                    .debtType(DebtType.CREDIT_CARD)
                    .principalAmount(new BigDecimal("5000.00"))
                    .currentBalance(new BigDecimal("3000.00"))
                    .interestRate(new BigDecimal("18.00"))
                    .startDate(LocalDate.now())
                    .minimumPayment(new BigDecimal("100.00"))
                    .build();
            debtService.createDebtFromDto(debt1);

            DebtDTO debt2 = DebtDTO.builder()
                    .userId(userContext.user().getUserId())
                    .debtName("Debt 2")
                    .debtType(DebtType.PERSONAL_LOAN)
                    .principalAmount(new BigDecimal("10000.00"))
                    .currentBalance(new BigDecimal("7000.00"))
                    .interestRate(new BigDecimal("10.00"))
                    .startDate(LocalDate.now())
                    .minimumPayment(new BigDecimal("200.00"))
                    .build();
            debtService.createDebtFromDto(debt2);

            BigDecimal total = debtService.getTotalDebtByUser(userContext.user());

            assertEquals(new BigDecimal("10000.00"), total);
        }
    }

    @Nested
    @DisplayName("Interest Calculations")
    class InterestCalculationTests {

        @Test
        @DisplayName("Should calculate monthly interest")
        void shouldCalculateMonthlyInterest() {
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal annualRate = new BigDecimal("12.00");

            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(balance, annualRate);

            // 10000 * (12/100/12) = 100
            assertEquals(0, new BigDecimal("100.00").compareTo(monthlyInterest));
        }

        @Test
        @DisplayName("Should calculate daily interest")
        void shouldCalculateDailyInterest() {
            BigDecimal balance = new BigDecimal("10000.00");
            BigDecimal annualRate = new BigDecimal("18.25");

            BigDecimal dailyInterest = debtService.calculateDailyInterest(balance, annualRate);

            // 10000 * (18.25/100/365) = 5
            assertEquals(0, new BigDecimal("5.00").compareTo(dailyInterest));
        }

        @Test
        @DisplayName("Should handle zero balance")
        void shouldHandleZeroBalance() {
            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(BigDecimal.ZERO, new BigDecimal("18.00"));
            BigDecimal dailyInterest = debtService.calculateDailyInterest(BigDecimal.ZERO, new BigDecimal("18.00"));

            assertEquals(0, BigDecimal.ZERO.compareTo(monthlyInterest));
            assertEquals(0, BigDecimal.ZERO.compareTo(dailyInterest));
        }

        @Test
        @DisplayName("Should handle zero interest rate")
        void shouldHandleZeroInterestRate() {
            BigDecimal monthlyInterest = debtService.calculateMonthlyInterest(new BigDecimal("10000.00"), BigDecimal.ZERO);
            BigDecimal dailyInterest = debtService.calculateDailyInterest(new BigDecimal("10000.00"), BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(monthlyInterest));
            assertEquals(0, BigDecimal.ZERO.compareTo(dailyInterest));
        }
    }
}
